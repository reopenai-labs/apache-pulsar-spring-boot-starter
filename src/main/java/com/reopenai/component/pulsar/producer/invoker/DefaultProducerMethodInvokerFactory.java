package com.reopenai.component.pulsar.producer.invoker;

import com.reopenai.component.pulsar.producer.ProducerContext;
import com.reopenai.component.pulsar.producer.annotation.DelayedDelivery;
import com.reopenai.component.pulsar.producer.annotation.ProducerMethod;
import com.reopenai.component.pulsar.resolve.MessageValueArgumentResolve;
import com.reopenai.component.pulsar.resolve.ProducerArgumentResolve;
import com.reopenai.component.pulsar.serialization.MessageConverter;
import com.reopenai.component.pulsar.serialization.MessageConverterRegister;
import com.reopenai.component.pulsar.utils.MessageUtil;
import com.reopenai.component.pulsar.utils.MethodParameterUtil;
import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.apache.pulsar.shade.com.fasterxml.jackson.core.type.TypeReference;
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

import static com.reopenai.component.pulsar.producer.invoker.ProducerMethodExecutor.AsyncProducerMethodExecutor;
import static com.reopenai.component.pulsar.producer.invoker.ProducerMethodExecutor.SyncProducerMethodExecutor;

/**
 * 默认的PProducerMethodInvokerFactory工厂的实现
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

    //消息转换器
    protected final MessageConverterRegister converterRegister;

    protected final List<ProducerMessageSupplierFactory> messageSuppliers;

    // 参数解析器
    protected final List<ProducerArgumentResolve> resolves;

    @Override
    public Map<Method, ProducerMethodInvoker> create(ProducerContext context) {
        Set<Method> methods = MethodIntrospector.selectMethods(context.getType(), this::isProducerMethod);
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
        Map<String, String> properties = MessageUtil.parseProperties(producerMethod.properties());
        // 解析延迟投递
        long delayedDelivery = parseDelayedDelivery(producerMethod);
        // 解析方法参数
        List<MethodParameter> parameters = MethodParameterUtil.parseMethodParameters(method);
        if (parameters.isEmpty()) {
            throw new BeanCreationException("方法缺少参数.method=" + method);
        }
        // 获取参数解析器
        List<ProducerArgumentResolve> argumentResolves = parseArgumentResolve(parameters);

        ProducerMessageSupplier messageSupplier = parseProducerMessageSupplier(context, method);

        // 构建Invoker实例
        return DefaultProducerMethodInvoker.builder()
                .method(method)
                .context(context)
                .executor(executor)
                .properties(properties)
                .methodParameters(parameters)
                .messageSupplier(messageSupplier)
                .argumentResolves(argumentResolves)
                .delayedTime(delayedDelivery)
                .build();
    }

    protected ProducerMessageSupplier parseProducerMessageSupplier(ProducerContext context, Method method) {
        for (ProducerMessageSupplierFactory supplierFactory : this.messageSuppliers) {
            if (supplierFactory.canProduce(context, method)) {
                return supplierFactory.create(context, method);
            }
        }
        ProducerMethod producerMethod = parseProducerMethod(method);
        MessageConverter converter = converterRegister.get(producerMethod.protocol());
        return new DefaultProducerMessageSupplier(converter);
    }

    protected ProducerMethod parseProducerMethod(Method method) {
        return method.getAnnotation(ProducerMethod.class);
    }


    protected List<ProducerArgumentResolve> parseArgumentResolve(List<MethodParameter> parameters) {
        // 只有一个参数的情况下直接当作MessageValue处理
        if (parameters.size() == 1) {
            for (ProducerArgumentResolve resolve : this.resolves) {
                if (resolve instanceof MessageValueArgumentResolve) {
                    return List.of(resolve);
                }
            }
        }
        ProducerArgumentResolve[] argumentResolves = new ProducerArgumentResolve[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            MethodParameter methodParameter = parameters.get(i);
            ProducerArgumentResolve matched = null;
            for (ProducerArgumentResolve resolve : this.resolves) {
                if (resolve.supportsParameter(methodParameter)) {
                    matched = resolve;
                    break;
                }
            }
            if (matched == null) {
                throw new BeanCreationException("无法解析PulsarProducer定义的参数.parameter=" + methodParameter);
            }
            argumentResolves[i] = matched;
        }
        return List.of(argumentResolves);
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
     * 创建执行器实例
     *
     * @param method  目标方法
     * @param context 上下文对象
     * @return 执行器实例
     */
    protected ProducerMethodExecutor createProducerMethodExecutor(Method method, ProducerContext context) {
        return isAsyncMode(method) ? new AsyncProducerMethodExecutor(context) : new SyncProducerMethodExecutor();
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
    protected static class DefaultProducerMessageSupplier implements ProducerMessageSupplier {

        private final MessageConverter converter;

        @Override
        public ProducerMessage get(ProducerContext context, Object[] arguments) {
            TypedMessageBuilder<byte[]> builder = context.getProducer().newMessage();
            return new ProducerMessage(converter, builder);
        }
    }

}
