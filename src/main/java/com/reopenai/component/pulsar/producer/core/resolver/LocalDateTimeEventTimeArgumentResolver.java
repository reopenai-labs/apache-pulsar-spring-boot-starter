package com.reopenai.component.pulsar.producer.core.resolver;

import com.reopenai.component.pulsar.annotation.MessageEventTime;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessage;
import org.springframework.core.MethodParameter;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * LocalDateTime格式的EventTime解析器
 *
 * @author Allen Huang
 */
public class LocalDateTimeEventTimeArgumentResolver implements ProducerArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MessageEventTime.class)
                && parameter.getParameterType() == LocalDateTime.class;
    }

    @Override
    public void resolveArgument(MethodParameter parameter, Object value, ProducerMessage message) {
        if (value != null) {
            LocalDateTime eventTime = (LocalDateTime) value;
            message.eventTime(eventTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
    }

}
