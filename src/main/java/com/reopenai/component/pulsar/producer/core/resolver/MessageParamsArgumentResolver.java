package com.reopenai.component.pulsar.producer.core.resolver;

import com.reopenai.component.pulsar.annotation.MessageParams;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessage;
import tools.jackson.core.type.TypeReference;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * 多个属性的参数解析器
 *
 * @author Allen Huang
 */
public class MessageParamsArgumentResolver implements ProducerArgumentResolver {

    private static final Type MATCH_TYPE = new TypeReference<Map<String, String>>() {
    }.getType();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MessageParams.class)
                && MATCH_TYPE.equals(parameter.getGenericParameterType());
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void resolveArgument(MethodParameter parameter, Object value, ProducerMessage message) {
        if (value instanceof Map properties) {
            message.properties(properties);
        }
    }

}
