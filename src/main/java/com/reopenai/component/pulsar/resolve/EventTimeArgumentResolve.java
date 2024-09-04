package com.reopenai.component.pulsar.resolve;

import com.reopenai.component.pulsar.annotation.MessageEventTime;
import com.reopenai.component.pulsar.producer.invoker.ProducerMessage;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

/**
 * EventTime参数解析器
 *
 * @author Allen Huang
 */
@Component
public class EventTimeArgumentResolve implements ProducerArgumentResolve, ConsumerArgumentResolve {

    @Override
    public boolean supportsParameter(MethodParameter parameterInfo) {
        return parameterInfo.hasParameterAnnotation(MessageEventTime.class)
                && (parameterInfo.getParameterType() == long.class || parameterInfo.getParameterType() == Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameterInfo, Message<byte[]> message) {
        return message.getEventTime();
    }

    @Override
    public void resolveArgument(MethodParameter parameterInfo, Object value, ProducerMessage message) {
        if (value != null) {
            message.eventTime((long) value);
        }
    }

}

