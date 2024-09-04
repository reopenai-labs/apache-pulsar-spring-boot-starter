package com.reopenai.component.pulsar.producer.invoker;

import com.reopenai.component.pulsar.producer.ProducerContext;

import java.lang.reflect.Method;

/**
 * @author Allen Huang
 */
public interface ProducerMessageSupplierFactory {

    boolean canProduce(ProducerContext context, Method method);

    ProducerMessageSupplier create(ProducerContext context, Method method);

}
