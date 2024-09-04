package com.reopenai.component.pulsar.consumer.core.resolver;

import com.reopenai.component.pulsar.annotation.MessageKey;
import com.reopenai.component.pulsar.consumer.core.invoker.ConsumerMessageRecord;
import org.springframework.core.MethodParameter;

/**
 * 消息Key的参数解析器.
 *
 * @author Allen Huang
 */
public class ConsumerMessageKeyArgumentResolver implements ConsumerArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MessageKey.class)
                && parameter.getParameterType() == String.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ConsumerMessageRecord record) {
        return record.getKey();
    }

}
