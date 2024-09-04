package com.reopenai.component.pulsar.producer.core.resolver;

import com.reopenai.component.pulsar.annotation.MessageParam;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessage;
import org.springframework.core.MethodParameter;

/**
 * 单个属性的参数解析器
 *
 * @author Allen Huang
 */
public class MessageParamArgumentResolver implements ProducerArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MessageParam.class)
                && parameter.getParameterType() == String.class;
    }

    @Override
    public void resolveArgument(MethodParameter parameter, Object value, ProducerMessage message) {
        if (value != null) {
            MessageParam param = parameter.getParameterAnnotation(MessageParam.class);
            message.property(param.value(), value.toString());
        }
    }

}
