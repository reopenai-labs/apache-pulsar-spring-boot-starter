package com.reopenai.component.pulsar.resolve;

import com.reopenai.component.pulsar.annotation.MessageParam;
import com.reopenai.component.pulsar.producer.invoker.ProducerMessage;
import org.apache.pulsar.client.api.Message;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

/**
 * 单个属性的参数解析器
 *
 * @author Allen Huang
 */
@Component
public class MessageParamArgumentResolve implements ProducerArgumentResolve, ConsumerArgumentResolve {

    @Override
    public boolean supportsParameter(MethodParameter parameterInfo) {
        return parameterInfo.hasParameterAnnotation(MessageParam.class)
                && parameterInfo.getParameterType() == String.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameterInfo, Message<byte[]> message) {
        MessageParam param = parameterInfo.getParameterAnnotation(MessageParam.class);
        String key = param.value();
        return message.getProperty(key);
    }

    @Override
    public void resolveArgument(MethodParameter parameterInfo, Object value, ProducerMessage message) {
        if (value != null) {
            MessageParam param = parameterInfo.getParameterAnnotation(MessageParam.class);
            message.property(param.value(), value.toString());
        }
    }

}
