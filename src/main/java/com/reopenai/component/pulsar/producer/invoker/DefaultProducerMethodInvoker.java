package com.reopenai.component.pulsar.producer.invoker;

import com.reopenai.component.pulsar.producer.ProducerContext;
import com.reopenai.component.pulsar.resolve.ProducerArgumentResolve;
import lombok.Builder;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于Executor的默认实现
 *
 * @author Allen Huang
 */
@Builder
public class DefaultProducerMethodInvoker implements ProducerMethodInvoker {
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
    protected final long delayedTime;
    /**
     * 消息的key
     */
    protected final String messageKey;
    /**
     * 预定义的消息属性properties
     */
    protected final Map<String, String> properties;
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
    protected final List<ProducerArgumentResolve> argumentResolves;

    protected final ProducerMessageSupplier messageSupplier;

    @Override
    public Object invoke(Object[] arguments) throws PulsarClientException {
        ProducerMessage message = messageSupplier.get(context, arguments);
        // 处理消息属性
        fillProperties(message);
        // 处理消息key
        fillMessageKey(message);
        // 处理延迟消息
        fillDelayedDelivery(message);
        // 解析消息参数
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            MethodParameter parameterInfo = methodParameters.get(i);
            ProducerArgumentResolve resolve = argumentResolves.get(i);
            resolve.resolveArgument(parameterInfo, argument, message);
        }
        return executor.send(message);
    }

    protected void fillDelayedDelivery(ProducerMessage message) {
        if (delayedTime > 0) {
            message.deliverAfter(delayedTime, TimeUnit.MILLISECONDS);
        }
    }

    protected void fillMessageKey(ProducerMessage message) {
        if (StringUtils.hasText(messageKey)) {
            message.key(messageKey);
        }
    }

    protected void fillProperties(ProducerMessage message) {
        if (properties != null && !properties.isEmpty()) {
            message.properties(properties);
        }
    }

}
