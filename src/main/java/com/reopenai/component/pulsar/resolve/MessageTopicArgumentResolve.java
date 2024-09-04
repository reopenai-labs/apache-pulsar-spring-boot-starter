package com.reopenai.component.pulsar.resolve;

import com.reopenai.component.pulsar.annotation.MessageTopic;
import org.apache.pulsar.client.api.Message;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

/**
 * 多个属性的参数解析器
 *
 * @author Allen Huang
 */
@Component
public class MessageTopicArgumentResolve implements ConsumerArgumentResolve {

    @Override
    public boolean supportsParameter(MethodParameter parameterInfo) {
        return parameterInfo.hasParameterAnnotation(MessageTopic.class)
                && parameterInfo.getParameter().getType() == String.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameterInfo, Message<byte[]> context) {
        return context.getTopicName();
    }

}
