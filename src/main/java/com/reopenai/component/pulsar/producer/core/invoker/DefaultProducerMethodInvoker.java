package com.reopenai.component.pulsar.producer.core.invoker;

import com.reopenai.component.pulsar.producer.core.ProducerContext;
import com.reopenai.component.pulsar.producer.core.handler.ProducerMessageHandler;
import com.reopenai.component.pulsar.producer.core.resolver.ProducerArgumentResolver;
import com.reopenai.component.pulsar.support.MessageHeaders;
import lombok.Builder;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于Executor的默认实现
 *
 * @author Allen Huang
 */
@Builder
public class DefaultProducerMethodInvoker implements ProducerMethodInvoker {

    /**
     * 当前主机名, 用于生成消息唯一键
     */
    private static final String HOSTNAME;

    /**
     * 自动生成的消息唯一键中, hostname 与 uuid 的分隔符
     */
    private static final String UNIQUE_KEY_SEPARATOR = "&";

    static {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = UUID.randomUUID().toString();
        }
        HOSTNAME = hostname;
    }

    /**
     * 上下文对象
     */
    protected final ProducerContext context;
    /**
     * 目标方法
     */
    protected final Method method;
    /**
     * 发送固定延迟消息的延迟时间，大于0时有效
     */
    protected final long deliveryDelayMillis;
    /**
     * 消息的key
     */
    protected final String messageKey;
    /**
     * 预定义的消息属性properties
     */
    protected final Map<String, String> properties;
    /**
     * 序列化协议, 写入消息属性供消费端反序列化
     */
    protected final String protocol;
    /**
     * 是否自动生成消息唯一键
     */
    protected final boolean autoUniqueKey;
    /**
     * 发送消息的执行器
     */
    protected final ProducerMethodExecutor executor;
    /**
     * 方法参数列表
     */
    protected final List<MethodParameter> methodParameters;
    /**
     * 参数解析器
     */
    protected final List<ProducerArgumentResolver> argumentResolvers;
    /**
     * 发送前/后的消息处理器(按Ordered顺序执行)
     */
    @Builder.Default
    protected final List<ProducerMessageHandler> handlers = List.of();

    protected final ProducerMessageFactory messageFactory;

    @Override
    public Object invoke(Object[] arguments) throws PulsarClientException {
        ProducerMessage message = messageFactory.get(context, arguments);
        // 处理预定义消息属性
        applyProperties(message);
        // 写入序列化协议(供消费端反序列化)
        applyProtocol(message);
        // 处理消息key
        applyMessageKey(message);
        // 处理延迟消息
        applyDeliveryDelay(message);
        // 自动生成消息唯一键
        applyUniqueKey(message);
        // 解析方法参数到消息
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            MethodParameter parameter = methodParameters.get(i);
            ProducerArgumentResolver resolver = argumentResolvers.get(i);
            resolver.resolveArgument(parameter, argument, message);
        }
        // 发送前处理器(异常隔离: handler 失败仅记录日志, 不阻断消息发送)
        for (ProducerMessageHandler handler : handlers) {
            invokeHandlerBefore(handler, message);
        }
        // 发送
        Object result;
        try {
            result = executor.send(message);
        } catch (PulsarClientException error) {
            for (ProducerMessageHandler handler : handlers) {
                invokeHandlerAfter(handler, message, null, error);
            }
            throw error;
        }
        // 发送后处理器(异常隔离)
        for (ProducerMessageHandler handler : handlers) {
            invokeHandlerAfter(handler, message, result, null);
        }
        return result;
    }

    protected void applyProtocol(ProducerMessage message) {
        if (StringUtils.hasText(protocol)) {
            message.property(MessageHeaders.PROTOCOL, protocol);
        }
    }

    protected void applyUniqueKey(ProducerMessage message) {
        if (autoUniqueKey) {
            message.property(MessageHeaders.UNIQUE_KEY, HOSTNAME + UNIQUE_KEY_SEPARATOR + UUID.randomUUID());
        }
    }

    protected void applyDeliveryDelay(ProducerMessage message) {
        if (deliveryDelayMillis > 0) {
            message.deliverAfter(deliveryDelayMillis, TimeUnit.MILLISECONDS);
        }
    }

    protected void applyMessageKey(ProducerMessage message) {
        if (StringUtils.hasText(messageKey)) {
            message.key(messageKey);
        }
    }

    protected void applyProperties(ProducerMessage message) {
        if (properties != null && !properties.isEmpty()) {
            message.properties(properties);
        }
    }

    /**
     * 调用发送前钩子, 异常隔离: handler 失败仅记录日志, 不阻断消息发送.
     */
    protected void invokeHandlerBefore(ProducerMessageHandler handler, ProducerMessage message) {
        try {
            handler.sendBefore(message, context);
        } catch (Exception e) {
            context.getLogger().warn("[Pulsar][Producer]sendBefore处理器异常, 已忽略.handler={}", handler.getClass().getName(), e);
        }
    }

    /**
     * 调用发送后钩子, 异常隔离: handler 失败仅记录日志, 不影响发送结果.
     */
    protected void invokeHandlerAfter(ProducerMessageHandler handler, ProducerMessage message, Object result, Throwable error) {
        try {
            handler.sendAfter(message, result, error, context);
        } catch (Exception e) {
            context.getLogger().error("[Pulsar][Producer]sendAfter处理器异常, 已忽略.handler={}", handler.getClass().getName(), e);
        }
    }

}
