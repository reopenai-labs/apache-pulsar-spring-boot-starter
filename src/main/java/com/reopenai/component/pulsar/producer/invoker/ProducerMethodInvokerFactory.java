package com.reopenai.component.pulsar.producer.invoker;

import com.reopenai.component.pulsar.producer.ProducerContext;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * {@link ProducerMethodInvoker}的创建工厂
 *
 * @author Allen Huang
 */
public interface ProducerMethodInvokerFactory {

    /**
     * 根据接口定义创建{@link ProducerMethodInvoker}实例
     *
     * @param context 上下文对象
     * @return PulsarProducerSender实例
     */
    Map<Method, ProducerMethodInvoker> create(ProducerContext context);


}
