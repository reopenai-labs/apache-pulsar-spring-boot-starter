package com.reopenai.component.pulsar.producer.invoker;

import com.reopenai.component.pulsar.producer.ProducerContext;
import org.apache.pulsar.client.api.TypedMessageBuilder;

/**
 * @author Allen Huang
 */
public interface TypedMessageBuilderFactory {

    TypedMessageBuilder<byte[]> create(ProducerContext context, Object[] arguments);

}
