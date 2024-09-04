package com.reopenai.component.pulsar.producer.invoker;

import com.reopenai.component.pulsar.producer.ProducerContext;

/**
 * @author Allen Huang
 */
public interface ProducerMessageSupplier {

    ProducerMessage get(ProducerContext context, Object[] arguments);

}
