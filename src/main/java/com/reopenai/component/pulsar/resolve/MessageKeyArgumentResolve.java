package com.reopenai.component.pulsar.resolve;

import com.reopenai.component.pulsar.annotation.MessageKey;
import com.reopenai.component.pulsar.producer.invoker.ProducerMessage;
import org.apache.pulsar.client.api.Message;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 消息KEY的参数解析器
 *
 * @author Allen Huang
 */
@Component
public class MessageKeyArgumentResolve implements ProducerArgumentResolve, ConsumerArgumentResolve {

    @Override
    public boolean supportsParameter(MethodParameter parameterInfo) {
        return parameterInfo.hasParameterAnnotation(MessageKey.class)
                && parameterInfo.getParameterType() == String.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameterInfo, Message<byte[]> message) {
        return message.getKey();
    }

    @Override
    public void resolveArgument(MethodParameter parameterInfo, Object value, ProducerMessage message) {
        String messageKey = ((String) value);
        if (StringUtils.hasText(messageKey)) {
            message.key(messageKey);
        }
    }
}

