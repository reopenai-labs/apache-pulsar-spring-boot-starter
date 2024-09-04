package com.reopenai.component.pulsar.producer.core.resolver;

import com.reopenai.component.pulsar.annotation.MessageKey;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessage;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;

/**
 * 消息KEY的参数解析器
 *
 * @author Allen Huang
 */
public class MessageKeyArgumentResolver implements ProducerArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MessageKey.class)
                && parameter.getParameterType() == String.class;
    }

    @Override
    public void resolveArgument(MethodParameter parameter, Object value, ProducerMessage message) {
        String messageKey = (String) value;
        if (StringUtils.hasText(messageKey)) {
            message.key(messageKey);
        }
    }

}
