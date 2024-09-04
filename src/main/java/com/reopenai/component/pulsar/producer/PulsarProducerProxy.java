package com.reopenai.component.pulsar.producer;

import com.reopenai.component.pulsar.producer.invoker.ProducerMethodInvoker;
import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Allen Huang
 */
public class PulsarProducerProxy implements InvocationHandler {

    private final Logger logger;

    private final Map<Method, ProducerMethodInvoker> invokers;

    public PulsarProducerProxy(ProducerContext context, Map<Method, ProducerMethodInvoker> invokers) {
        this.invokers = invokers;
        this.logger = context.getLogger();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        if (Object.class.equals(declaringClass)) {
            return method.invoke(this, args);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("[PulsarProducer]requesting message.args:{}", Arrays.asList(args));
        }
        ProducerMethodInvoker invoker = invokers.get(method);
        if (invoker != null) {
            return invoker.invoke(args);
        }
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        throw new IllegalStateException("Unexpected method invocation: " + method);
    }

}
