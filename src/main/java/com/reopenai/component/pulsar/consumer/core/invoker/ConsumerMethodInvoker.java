package com.reopenai.component.pulsar.consumer.core.invoker;

import com.reopenai.component.pulsar.consumer.core.ConsumerContext;
import com.reopenai.component.pulsar.consumer.core.handler.ConsumerMessageHandler;
import com.reopenai.component.pulsar.consumer.core.resolver.ConsumerArgumentResolver;
import com.reopenai.component.pulsar.serialization.MessageConverter;
import com.reopenai.component.pulsar.serialization.MessageConverterRegistry;
import com.reopenai.component.pulsar.support.MessageHeaders;
import lombok.Builder;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 消费者方法调用器. 一个 {@code @ConsumerHandler} 方法对应一个本实例, 负责:
 * <ol>
 *   <li>属性匹配({@code matchProperties}): 不匹配则跳过, 交由下一个handler</li>
 *   <li>参数解析与反序列化: 协议优先取消息属性 {@link MessageHeaders#PROTOCOL}(生产者契约), 其次handler默认协议</li>
 *   <li>反射调用handler方法</li>
 *   <li>ack策略: 方法含 {@link ConsumerContext} 参数为手动ack, 否则自动ack(成功ack/失败nack重试)</li>
 *   <li>错误处理: {@code noNegativeFor}异常视为成功; {@code ignoreDeserializeFail}反序列化失败可忽略</li>
 * </ol>
 *
 * @author Allen Huang
 */
@Builder
public class ConsumerMethodInvoker {

    private final Logger logger;

    private final Method method;

    private final Object instance;

    private final MessageConverterRegistry converterRegistry;

    private final String defaultProtocol;

    private final List<MethodParameter> parameters;

    /**
     * 每个参数预匹配的解析器; {@link ConsumerContext} 类型参数位置为null(由invoker直接注入context)
     */
    private final List<ConsumerArgumentResolver> argumentResolvers;

    private final Map<String, String> matchProperties;

    private final Class<? extends Throwable>[] noNegativeFor;

    private final boolean ignoreDeserializeFail;

    /**
     * 方法是否含 {@link ConsumerContext} 参数 → 手动ack模式
     */
    private final boolean manualAck;

    /**
     * 消费前后处理器(按Ordered顺序执行, 异常隔离)
     */
    @Builder.Default
    private final List<ConsumerMessageHandler> handlers = List.of();

    /**
     * 处理一条消息.
     *
     * @param context 消费上下文
     * @return true=属性匹配并已处理(无论成功失败); false=属性不匹配, 应交由下一个handler
     */
    public boolean handle(ConsumerContext context) {
        Message<byte[]> message = context.getMessage();
        if (!matchProperties(message)) {
            return false;
        }
        // 协议: 优先消息属性(生产者契约), 其次handler默认协议
        String protocol = message.getProperty(MessageHeaders.PROTOCOL);
        if (!StringUtils.hasText(protocol)) {
            protocol = defaultProtocol;
        }
        ConsumerMessageRecord record;
        Object[] params = new Object[parameters.size()];
        try {
            // converter 获取与反序列化统一纳入下方 catch: 协议不存在时也可被 ignoreDeserializeFail 兜底
            MessageConverter converter = converterRegistry.get(protocol);
            record = new ConsumerMessageRecord(message, converter);
            for (int i = 0; i < parameters.size(); i++) {
                MethodParameter parameter = parameters.get(i);
                if (ConsumerContext.class.isAssignableFrom(parameter.getParameterType())) {
                    params[i] = context;
                    continue;
                }
                params[i] = argumentResolvers.get(i).resolveArgument(parameter, record);
            }
        } catch (Exception e) {
            return onDeserializeFail(context, e);
        }

        // 消费前钩子(异常隔离)
        invokeConsumeBefore(record, context);

        Throwable invokeError = null;
        try {
            method.invoke(instance, params);
        } catch (InvocationTargetException e) {
            invokeError = e.getCause() == null ? e : e.getCause();
        } catch (IllegalAccessException e) {
            invokeConsumeAfter(record, context, e);
            throw new IllegalStateException("消费者方法调用失败: " + method, e);
        }

        // 消费后钩子(成功 invokeError=null, 业务异常 invokeError=cause)
        invokeConsumeAfter(record, context, invokeError);

        if (invokeError != null) {
            return onInvokeError(context, invokeError);
        }

        // 自动ack模式: 成功后ack
        if (!manualAck) {
            acknowledge(context);
        }
        return true;
    }

    private boolean onDeserializeFail(ConsumerContext context, Exception e) {
        if (ignoreDeserializeFail) {
            logger.warn("[Pulsar][Consumer]反序列化错误,消息将被忽略.topic={},错误={}",
                    context.getConsumer().getTopic(), e.getMessage());
            acknowledge(context);
            return true;
        }
        negativeAcknowledge(context, e);
        return true;
    }

    private boolean onInvokeError(ConsumerContext context, Throwable e) {
        if (noNegativeFor != null) {
            for (Class<? extends Throwable> noNeg : noNegativeFor) {
                if (noNeg.isInstance(e)) {
                    logger.warn("[Pulsar][Consumer]方法抛出noNegativeFor异常,视为消费成功.topic={}",
                            context.getConsumer().getTopic(), e);
                    if (!manualAck) {
                        acknowledge(context);
                    }
                    return true;
                }
            }
        }
        negativeAcknowledge(context, e);
        return true;
    }

    private void acknowledge(ConsumerContext context) {
        try {
            context.doAcknowledge();
        } catch (PulsarClientException e) {
            logger.error("[Pulsar][Consumer]ack失败.topic={}", context.getConsumer().getTopic(), e);
        }
    }

    private void negativeAcknowledge(ConsumerContext context, Throwable e) {
        logger.error("[Pulsar][Consumer]消费失败,消息将被重试.topic={}", context.getConsumer().getTopic(), e);
        try {
            context.doNegativeAcknowledge();
        } catch (PulsarClientException ex) {
            logger.error("[Pulsar][Consumer]nack失败.topic={}", context.getConsumer().getTopic(), ex);
        }
    }

    /**
     * 调用消费前钩子, 异常隔离: 处理器失败仅记录日志, 不影响消费.
     */
    private void invokeConsumeBefore(ConsumerMessageRecord record, ConsumerContext context) {
        for (ConsumerMessageHandler handler : handlers) {
            try {
                handler.consumeBefore(record, context);
            } catch (Exception e) {
                logger.warn("[Pulsar][Consumer]consumeBefore处理器异常, 已忽略.handler={}", handler.getClass().getName(), e);
            }
        }
    }

    /**
     * 调用消费后钩子, 异常隔离: 处理器失败仅记录日志, 不影响 ack.
     */
    private void invokeConsumeAfter(ConsumerMessageRecord record, ConsumerContext context, Throwable error) {
        for (ConsumerMessageHandler handler : handlers) {
            try {
                handler.consumeAfter(record, context, error);
            } catch (Exception e) {
                logger.error("[Pulsar][Consumer]consumeAfter处理器异常, 已忽略.handler={}", handler.getClass().getName(), e);
            }
        }
    }

    private boolean matchProperties(Message<byte[]> message) {
        if (matchProperties == null || matchProperties.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, String> entry : matchProperties.entrySet()) {
            if (!Objects.equals(entry.getValue(), message.getProperty(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否为兜底处理器(未声明属性匹配条件, 处理所有消息). 排序时兜底处理器置于末尾,
     * 避免抢占应由特定处理器处理的消息.
     */
    public boolean isFallback() {
        return matchProperties == null || matchProperties.isEmpty();
    }

}
