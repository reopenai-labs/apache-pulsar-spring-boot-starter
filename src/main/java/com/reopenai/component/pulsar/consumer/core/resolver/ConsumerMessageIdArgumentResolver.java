package com.reopenai.component.pulsar.consumer.core.resolver;

import com.reopenai.component.pulsar.annotation.RawMessageId;
import com.reopenai.component.pulsar.consumer.core.invoker.ConsumerMessageRecord;
import org.apache.pulsar.client.api.MessageId;
import org.springframework.core.MethodParameter;

/**
 * 消息ID的参数解析器: 注入 Pulsar 原始消息 ID.
 *
 * @author Allen Huang
 */
public class ConsumerMessageIdArgumentResolver implements ConsumerArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RawMessageId.class)
                && parameter.getParameterType() == MessageId.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ConsumerMessageRecord record) {
        return record.getMessageId();
    }

}
