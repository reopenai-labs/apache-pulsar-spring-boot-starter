package com.reopenai.component.pulsar.resolve;

import com.reopenai.component.pulsar.annotation.MsgID;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

/**
 * 消息id解析器
 *
 * @author Allen Huang
 */
@Component
public class MessageIdArgumentResolve implements ConsumerArgumentResolve {

    @Override
    public boolean supportsParameter(MethodParameter parameterInfo) {
        return parameterInfo.hasParameterAnnotation(MsgID.class)
                && parameterInfo.getParameterType() == MessageId.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameterInfo, Message<byte[]> message) {
        return message.getMessageId();
    }

}
