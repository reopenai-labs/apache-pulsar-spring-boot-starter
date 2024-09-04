package com.reopenai.component.pulsar.producer.core.resolver;

import com.reopenai.component.pulsar.annotation.MessageEventTime;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessage;
import org.springframework.core.MethodParameter;

/**
 * EventTime参数解析器
 *
 * @author Allen Huang
 */
public class EventTimeArgumentResolver implements ProducerArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MessageEventTime.class)
                && (parameter.getParameterType() == long.class || parameter.getParameterType() == Long.class);
    }

    @Override
    public void resolveArgument(MethodParameter parameter, Object value, ProducerMessage message) {
        if (value != null) {
            message.eventTime((long) value);
        }
    }

}
