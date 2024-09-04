package com.reopenai.component.pulsar.resolve;

import com.reopenai.component.pulsar.annotation.MessageEventTime;
import com.reopenai.component.pulsar.producer.invoker.ProducerMessage;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * LocalDateTime格式的EventTime解析器
 *
 * @author Allen Huang
 */
@Component
public class LocalDateTimeEventTimeArgumentResolve implements ProducerArgumentResolve, ConsumerArgumentResolve {

    private static final ZoneOffset offset = OffsetDateTime.now().getOffset();

    @Override
    public boolean supportsParameter(MethodParameter parameterInfo) {
        return parameterInfo.hasParameterAnnotation(MessageEventTime.class)
                && parameterInfo.getParameterType() == LocalDateTime.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameterInfo, Message<byte[]> message) {
        long eventTime = message.getEventTime();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(eventTime), offset);
    }

    @Override
    public void resolveArgument(MethodParameter parameterInfo, Object value, ProducerMessage message) {
        if (value != null) {
            LocalDateTime eventTime = (LocalDateTime) value;
            message.eventTime(eventTime.toEpochSecond(offset));
        }
    }

}