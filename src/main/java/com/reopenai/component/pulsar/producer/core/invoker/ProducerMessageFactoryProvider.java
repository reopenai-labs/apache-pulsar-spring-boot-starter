package com.reopenai.component.pulsar.producer.core.invoker;

import com.reopenai.component.pulsar.producer.core.ProducerContext;

import java.lang.reflect.Method;

/**
 * @author Allen Huang
 */
public interface ProducerMessageFactoryProvider {

    boolean supports(ProducerContext context, Method method);

    ProducerMessageFactory create(ProducerContext context, Method method);

}
