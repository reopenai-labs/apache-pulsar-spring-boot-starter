package com.reopenai.component.pulsar.resolve;


import com.reopenai.component.pulsar.annotation.MessageParams;
import com.reopenai.component.pulsar.producer.invoker.ProducerMessage;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.shade.com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * 多个属性的参数解析器
 *
 * @author Allen Huang
 */
@Component
public class MessageParamsArgumentResolve implements ProducerArgumentResolve, ConsumerArgumentResolve {

    private static final Type MATCH_TYPE = new TypeReference<Map<String, String>>() {
    }.getType();

    @Override
    public boolean supportsParameter(MethodParameter parameterInfo) {
        return parameterInfo.hasParameterAnnotation(MessageParams.class)
                && MATCH_TYPE.equals(parameterInfo.getGenericParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameterInfo, Message<byte[]> message) {
        return message.getProperties();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void resolveArgument(MethodParameter parameterInfo, Object value, ProducerMessage message) {
        if (value instanceof Map params) {
            message.properties(params);
        }
    }

}
