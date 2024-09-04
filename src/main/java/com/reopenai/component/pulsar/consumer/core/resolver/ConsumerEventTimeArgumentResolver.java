package com.reopenai.component.pulsar.consumer.core.resolver;

import com.reopenai.component.pulsar.annotation.MessageEventTime;
import com.reopenai.component.pulsar.consumer.core.invoker.ConsumerMessageRecord;
import org.springframework.core.MethodParameter;

/**
 * EventTime参数解析器(毫秒时间戳).
 *
 * @author Allen Huang
 */
public class ConsumerEventTimeArgumentResolver implements ConsumerArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MessageEventTime.class)
                && (parameter.getParameterType() == long.class || parameter.getParameterType() == Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ConsumerMessageRecord record) {
        return record.getEventTime();
    }

}
