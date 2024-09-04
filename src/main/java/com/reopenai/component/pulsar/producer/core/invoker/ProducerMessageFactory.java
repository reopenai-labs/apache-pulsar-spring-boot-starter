package com.reopenai.component.pulsar.producer.core.invoker;

import com.reopenai.component.pulsar.producer.core.ProducerContext;

/**
 * @author Allen Huang
 */
public interface ProducerMessageFactory {

    ProducerMessage get(ProducerContext context, Object[] arguments);

}
