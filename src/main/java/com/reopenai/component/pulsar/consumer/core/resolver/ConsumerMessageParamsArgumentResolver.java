package com.reopenai.component.pulsar.consumer.core.resolver;

import com.reopenai.component.pulsar.annotation.MessageParams;
import com.reopenai.component.pulsar.consumer.core.invoker.ConsumerMessageRecord;
import tools.jackson.core.type.TypeReference;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * 多个属性的参数解析器: 将消息全部属性以 {@code Map<String,String>} 注入.
 *
 * @author Allen Huang
 */
public class ConsumerMessageParamsArgumentResolver implements ConsumerArgumentResolver {

    private static final Type MATCH_TYPE = new TypeReference<Map<String, String>>() {
    }.getType();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MessageParams.class)
                && MATCH_TYPE.equals(parameter.getGenericParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ConsumerMessageRecord record) {
        return record.getProperties();
    }

}
