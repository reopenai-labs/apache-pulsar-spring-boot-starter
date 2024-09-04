package com.reopenai.component.pulsar.producer.core.invoker;

import com.reopenai.component.pulsar.producer.core.ProducerContext;
import com.reopenai.component.pulsar.producer.core.TransactionPhase;
import com.reopenai.component.pulsar.annotation.DelayedDelivery;
import com.reopenai.component.pulsar.annotation.ProducerMethod;
import com.reopenai.component.pulsar.producer.core.handler.ProducerMessageHandler;
import com.reopenai.component.pulsar.producer.core.resolver.MessageValueArgumentResolver;
import com.reopenai.component.pulsar.producer.core.resolver.ProducerArgumentResolver;
import com.reopenai.component.pulsar.serialization.MessageConverter;
import com.reopenai.component.pulsar.serialization.MessageConverterRegistry;
import com.reopenai.component.pulsar.support.MessagePropertyUtil;
import com.reopenai.component.pulsar.support.MethodParameterUtil;
import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import tools.jackson.core.type.TypeReference;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.reopenai.component.pulsar.producer.core.invoker.ProducerMethodExecutor.AsyncProducerMethodExecutor;
import static com.reopenai.component.pulsar.producer.core.invoker.ProducerMethodExecutor.SyncProducerMethodExecutor;

/**
 * 默认的ProducerMethodInvokerFactory工厂的实现
 *
 * @author Allen Huang
 */
@RequiredArgsConstructor
public class DefaultProducerMethodInvokerFactory implements ProducerMethodInvokerFactory {

    //同步发送的返回类型
    private static final Type SYNC_TYPE = MessageId.class;

    //异步发送的返回类型
    private static final Type ASYNC_TYPE = new TypeReference<CompletableFuture<MessageId>>() {
    }.getType();

    //消息转换器注册表
    protected final MessageConverterRegistry converterRegistry;

    protected final List<ProducerMessageFactoryProvider> messageFactoryProviders;

    // 参数解析器
    protected final List<ProducerArgumentResolver> resolvers;

    // 消息处理器(发送前后钩子)
    protected final List<ProducerMessageHandler> handlers;

    @Override
    public Map<Method, ProducerMethodInvoker> create(ProducerContext context) {
        Set<Method> methods = MethodIntrospector.selectMethods(context.getProducerType(), this::isProducerMethod);
        Map<Method, ProducerMethodInvoker> invokers = new HashMap<>(methods.size());
        for (Method method : methods) {
            ProducerMethodInvoker invoker = create(method, context);
            invokers.put(method, invoker);
        }
        return invokers;
    }

    protected ProducerMethodInvoker create(Method method, ProducerContext context) {
        ProducerMethod producerMethod = parseProducerMethod(method);
        ProducerMethodExecutor executor = createProducerMethodExecutor(method, context);
        // 解析properties
        Map<String, String> properties = MessagePropertyUtil.parseProperties(producerMethod.properties());
        // 解析延迟投递
        long deliveryDelayMillis = parseDelayedDelivery(producerMethod);
        // 解析方法参数
        List<MethodParameter> parameters = MethodParameterUtil.parseMethodParameters(method);
        if (parameters.isEmpty()) {
            throw new BeanCreationException("方法缺少参数.method=" + method);
        }
        // 获取参数解析器
        List<ProducerArgumentResolver> argumentResolvers = parseArgumentResolvers(parameters);

        ProducerMessageFactory messageFactory = resolveMessageFactory(context, method);

        // 构建Invoker实例
        return DefaultProducerMethodInvoker.builder()
                .method(method)
                .context(context)
                .executor(executor)
                .properties(properties)
                .protocol(producerMethod.protocol())
                .autoUniqueKey(producerMethod.autoUniqueKey())
                .messageKey(producerMethod.messageKey())
                .methodParameters(parameters)
                .messageFactory(messageFactory)
                .argumentResolvers(argumentResolvers)
                .handlers(handlers)
                .deliveryDelayMillis(deliveryDelayMillis)
                .build();
    }

    protected ProducerMessageFactory resolveMessageFactory(ProducerContext context, Method method) {
        for (ProducerMessageFactoryProvider messageFactoryProvider : this.messageFactoryProviders) {
            if (messageFactoryProvider.supports(context, method)) {
                return messageFactoryProvider.create(context, method);
            }
        }
        ProducerMethod producerMethod = parseProducerMethod(method);
        MessageConverter converter = converterRegistry.get(producerMethod.protocol());
        return new DefaultProducerMessageFactory(converter);
    }

    protected ProducerMethod parseProducerMethod(Method method) {
        return method.getAnnotation(ProducerMethod.class);
    }


    protected List<ProducerArgumentResolver> parseArgumentResolvers(List<MethodParameter> parameters) {
        // 单参数且无任何注解: 默认作为消息体(便捷写法, 如 send(Order)); 有注解则走正常匹配
        if (parameters.size() == 1 && parameters.get(0).getParameterAnnotations().length == 0) {
            return List.of(resolveMessageValueResolver());
        }
        ProducerArgumentResolver[] argumentResolvers = new ProducerArgumentResolver[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            MethodParameter methodParameter = parameters.get(i);
            ProducerArgumentResolver matched = null;
            for (ProducerArgumentResolver resolver : this.resolvers) {
                if (resolver.supportsParameter(methodParameter)) {
                    matched = resolver;
                    break;
                }
            }
            if (matched == null) {
                throw new BeanCreationException("无法解析PulsarProducer定义的参数.parameter=" + methodParameter);
            }
            argumentResolvers[i] = matched;
        }
        return List.of(argumentResolvers);
    }

    /**
     * 取出消息体解析器, 用于单参数无注解的默认消息体场景.
     */
    protected ProducerArgumentResolver resolveMessageValueResolver() {
        for (ProducerArgumentResolver resolver : this.resolvers) {
            if (resolver instanceof MessageValueArgumentResolver) {
                return resolver;
            }
        }
        throw new BeanCreationException("缺少 MessageValueArgumentResolver, 无法解析默认消息体参数");
    }

    protected long parseDelayedDelivery(ProducerMethod producerMethod) {
        DelayedDelivery delayed = producerMethod.delayed();
        if (delayed.value() > 0) {
            return delayed.timeUnit().toMillis(delayed.value());
        }
        return 0;
    }


    /**
     * 检查是否是ProducerMethod方法
     *
     * @param method 待检查的方法
     * @return 如果是则返回true，否则返回false
     */
    protected boolean isProducerMethod(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, ProducerMethod.class);
    }

    /**
     * 创建执行器实例: 按方法返回类型选择 Sync/Async, 若声明了 AFTER_COMMIT 事务策略则用装饰器包装.
     *
     * @param method  目标方法
     * @param context 上下文对象
     * @return 执行器实例
     */
    protected ProducerMethodExecutor createProducerMethodExecutor(Method method, ProducerContext context) {
        boolean async = isAsyncMode(method);
        ProducerMethodExecutor executor = async ? new AsyncProducerMethodExecutor(context) : new SyncProducerMethodExecutor();
        ProducerMethod producerMethod = parseProducerMethod(method);
        if (producerMethod.transaction() == TransactionPhase.AFTER_COMMIT) {
            return new TransactionAwareProducerMethodExecutor(executor, async, context);
        }
        return executor;
    }

    /**
     * 判断是否为异步发送模式
     *
     * @param method 目标方法
     * @return 如果是异步模式则返回true，否则返回false
     */
    protected boolean isAsyncMode(Method method) {
        Type returnType = method.getGenericReturnType();
        if (ASYNC_TYPE.equals(returnType)) {
            return true;
        }
        if (SYNC_TYPE.equals(returnType) || Void.TYPE == returnType) {
            return false;
        }
        throw new BeanCreationException("Pulsar生产者定义错误.方法的返回值只能是void、MessageId、CompletableFuture<MessageId>");
    }

    @RequiredArgsConstructor
    protected static class DefaultProducerMessageFactory implements ProducerMessageFactory {

        private final MessageConverter converter;

        @Override
        public ProducerMessage get(ProducerContext context, Object[] arguments) {
            TypedMessageBuilder<byte[]> builder = context.getProducer().newMessage();
            return new ProducerMessage(converter, builder);
        }
    }

}
