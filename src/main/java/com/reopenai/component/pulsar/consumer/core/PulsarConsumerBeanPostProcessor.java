package com.reopenai.component.pulsar.consumer.core;

import com.reopenai.component.pulsar.annotation.ConsumerHandler;
import com.reopenai.component.pulsar.annotation.ConsumerHandlers;
import com.reopenai.component.pulsar.annotation.PulsarConsumer;
import com.reopenai.component.pulsar.consumer.core.invoker.ConsumerMethodInvoker;
import com.reopenai.component.pulsar.consumer.core.handler.ConsumerMessageHandler;
import com.reopenai.component.pulsar.consumer.core.resolver.ConsumerArgumentResolver;
import com.reopenai.component.pulsar.consumer.core.resolver.ConsumerMessageValueArgumentResolver;
import com.reopenai.component.pulsar.serialization.MessageConverterRegistry;
import com.reopenai.component.pulsar.support.MessagePropertyUtil;
import com.reopenai.component.pulsar.support.MethodParameterUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 消费者扫描入口. 扫描标注 {@link PulsarConsumer} 的 Bean, 解析其中的 {@link ConsumerHandler} 方法,
 * 委托 {@link ConsumerConfiguration} 配置、{@link ConsumerRegistry} 管理生命周期, 创建 Pulsar
 * {@link Consumer} 并以 {@code MessageListener} 模式启动消费.
 * <p>
 * 消息到达时遍历 invoker, 首个属性匹配的 handler 处理(兜底 handler 排末尾);
 * 具体的参数解析、反序列化、ack/错误处理交由 {@link ConsumerMethodInvoker}.
 *
 * @author Allen Huang
 */
@Slf4j
public class PulsarConsumerBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, Ordered {

    private ApplicationContext applicationContext;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        PulsarConsumer annotation = AnnotatedElementUtils.findMergedAnnotation(targetClass, PulsarConsumer.class);
        if (annotation == null) {
            return bean;
        }
        List<ConsumerMethodInvoker> invokers = parseHandlers(targetClass, bean);
        if (invokers.isEmpty()) {
            log.warn("[Pulsar][Consumer]{} 未找到任何 @ConsumerHandler 方法, 跳过消费者创建", targetClass.getName());
            return bean;
        }
        startConsumer(annotation, invokers, targetClass);
        return bean;
    }

    /**
     * 解析目标类中所有 {@code @ConsumerHandler}/@ConsumerHandlers 方法, 构建 Invoker 列表.
     * 兜底处理器(无属性匹配条件)排在末尾, 避免抢占应由特定处理器处理的消息.
     */
    private List<ConsumerMethodInvoker> parseHandlers(Class<?> targetClass, Object bean) {
        Map<Method, Set<ConsumerHandler>> methodHandlers = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<Set<ConsumerHandler>>) method -> {
                    Set<ConsumerHandler> handlers = new LinkedHashSet<>();
                    ConsumerHandler single = AnnotatedElementUtils.findMergedAnnotation(method, ConsumerHandler.class);
                    if (single != null) {
                        handlers.add(single);
                    }
                    ConsumerHandlers multi = AnnotationUtils.findAnnotation(method, ConsumerHandlers.class);
                    if (multi != null) {
                        Collections.addAll(handlers, multi.value());
                    }
                    return handlers.isEmpty() ? null : handlers;
                });
        List<ConsumerArgumentResolver> resolvers = getResolvers();
        List<ConsumerMessageHandler> messageHandlers = getConsumerMessageHandlers();
        List<ConsumerMethodInvoker> invokers = new ArrayList<>();
        for (Map.Entry<Method, Set<ConsumerHandler>> entry : methodHandlers.entrySet()) {
            for (ConsumerHandler handler : entry.getValue()) {
                invokers.add(buildInvoker(entry.getKey(), bean, handler, resolvers, messageHandlers));
            }
        }
        invokers.sort(Comparator.comparing(ConsumerMethodInvoker::isFallback));
        return invokers;
    }

    private ConsumerMethodInvoker buildInvoker(Method method, Object bean, ConsumerHandler handler,
                                               List<ConsumerArgumentResolver> resolvers,
                                               List<ConsumerMessageHandler> messageHandlers) {
        method.setAccessible(true);
        List<MethodParameter> parameters = MethodParameterUtil.parseMethodParameters(method);
        List<ConsumerArgumentResolver> matched = new ArrayList<>(parameters.size());
        boolean manualAck = false;
        for (MethodParameter param : parameters) {
            if (ConsumerContext.class.isAssignableFrom(param.getParameterType())) {
                matched.add(null);
                manualAck = true;
                continue;
            }
            ConsumerArgumentResolver resolver = matchResolver(param, resolvers);
            // 单参数无注解: 默认作为消息体(便捷写法, 如 handle(Order)), 与Producer对称
            if (resolver == null && parameters.size() == 1 && param.getParameterAnnotations().length == 0) {
                resolver = findMessageValueResolver(resolvers);
            }
            if (resolver == null) {
                throw new BeanCreationException("无法解析消费者参数 " + param + " (方法 " + method + ")");
            }
            matched.add(resolver);
        }
        return ConsumerMethodInvoker.builder()
                .method(method)
                .instance(bean)
                .converterRegistry(getConverterRegistry())
                .defaultProtocol(handler.protocol())
                .parameters(parameters)
                .argumentResolvers(matched)
                .matchProperties(MessagePropertyUtil.parseProperties(handler.properties()))
                .noNegativeFor(handler.noNegativeFor())
                .ignoreDeserializeFail(handler.ignoreDeserializeFail())
                .manualAck(manualAck)
                .handlers(messageHandlers)
                .build();
    }

    private void startConsumer(PulsarConsumer annotation, List<ConsumerMethodInvoker> invokers, Class<?> targetClass) {
        try {
            PulsarClient client = applicationContext.getBean(PulsarClient.class);
            ConsumerBuilder<byte[]> builder = client.newConsumer(Schema.BYTES);
            getConsumerConfiguration().configure(builder, annotation);
            builder.messageListener((consumer, msg) -> dispatch(consumer, msg, invokers));
            Consumer<byte[]> consumer = builder.subscribe();
            getConsumerRegistry().register(consumer);
            log.info("[Pulsar][Consumer]消费者已启动. bean={}, topics={}",
                    targetClass.getSimpleName(), Arrays.toString(annotation.topicNames()));
        } catch (PulsarClientException e) {
            throw new BeanCreationException("创建Pulsar消费者失败: " + targetClass.getName(), e);
        }
    }

    private void dispatch(Consumer<byte[]> consumer, Message<byte[]> msg, List<ConsumerMethodInvoker> invokers) {
        ConsumerContext context = new ConsumerContext(consumer, msg);
        try {
            for (ConsumerMethodInvoker invoker : invokers) {
                if (invoker.handle(context)) {
                    return;
                }
            }
            // 无handler匹配, ack丢弃避免堆积
            log.warn("[Pulsar][Consumer]无handler匹配消息, ack丢弃.topic={}", msg.getTopicName());
            context.doAcknowledge();
        } catch (Exception e) {
            log.error("[Pulsar][Consumer]消息处理异常.topic={}", msg.getTopicName(), e);
            try {
                context.doNegativeAcknowledge();
            } catch (PulsarClientException ex) {
                log.warn("[Pulsar][Consumer]nack失败, 消息可能丢失(消费者可能已关闭).topic={}", msg.getTopicName(), ex);
            }
        }
    }

    private ConsumerArgumentResolver matchResolver(MethodParameter param, List<ConsumerArgumentResolver> resolvers) {
        for (ConsumerArgumentResolver resolver : resolvers) {
            if (resolver.supportsParameter(param)) {
                return resolver;
            }
        }
        return null;
    }

    /**
     * 取出消息体解析器, 用于单参数无注解的默认消息体场景(与Producer对称).
     */
    private ConsumerArgumentResolver findMessageValueResolver(List<ConsumerArgumentResolver> resolvers) {
        for (ConsumerArgumentResolver resolver : resolvers) {
            if (resolver instanceof ConsumerMessageValueArgumentResolver) {
                return resolver;
            }
        }
        return null;
    }

    private List<ConsumerArgumentResolver> getResolvers() {
        return new ArrayList<>(applicationContext.getBeansOfType(ConsumerArgumentResolver.class).values());
    }

    private MessageConverterRegistry getConverterRegistry() {
        return applicationContext.getBean(MessageConverterRegistry.class);
    }

    private ConsumerConfiguration getConsumerConfiguration() {
        return applicationContext.getBean(ConsumerConfiguration.class);
    }

    private ConsumerRegistry getConsumerRegistry() {
        return applicationContext.getBean(ConsumerRegistry.class);
    }

    private List<ConsumerMessageHandler> getConsumerMessageHandlers() {
        return new ArrayList<>(applicationContext.getBeansOfType(ConsumerMessageHandler.class).values());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

}
