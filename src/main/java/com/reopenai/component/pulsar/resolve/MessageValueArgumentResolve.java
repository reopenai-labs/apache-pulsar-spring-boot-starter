package com.reopenai.component.pulsar.resolve;

import com.reopenai.component.pulsar.annotation.MessageValue;
import com.reopenai.component.pulsar.producer.invoker.ProducerMessage;
import org.apache.pulsar.client.api.Message;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

/**
 * 消息内容解析器
 *
 * @author Allen Huang
 */
@Component
public class MessageValueArgumentResolve implements ProducerArgumentResolve, ConsumerArgumentResolve {

    @Override
    public boolean supportsParameter(MethodParameter parameterInfo) {
        return parameterInfo.hasParameterAnnotation(MessageValue.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameterInfo, Message<byte[]> context) {
        return parameterInfo;
    }

    @Override
    public void resolveArgument(MethodParameter parameterInfo, Object value, ProducerMessage message) {
        if (value != null) {
            message.value(parameterInfo, value);
        }
    }

}
