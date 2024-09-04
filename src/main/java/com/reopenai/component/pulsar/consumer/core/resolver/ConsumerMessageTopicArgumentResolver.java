package com.reopenai.component.pulsar.consumer.core.resolver;

import com.reopenai.component.pulsar.annotation.MessageTopic;
import com.reopenai.component.pulsar.consumer.core.invoker.ConsumerMessageRecord;
import org.springframework.core.MethodParameter;

/**
 * Topic名称的参数解析器.
 *
 * @author Allen Huang
 */
public class ConsumerMessageTopicArgumentResolver implements ConsumerArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MessageTopic.class)
                && parameter.getParameterType() == String.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ConsumerMessageRecord record) {
        return record.getTopicName();
    }

}
