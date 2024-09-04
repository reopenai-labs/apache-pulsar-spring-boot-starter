package com.reopenai.component.pulsar.serialization;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * 基于Jackson实现的JSON序列化解析器
 *
 * @author Allen Huang
 */
@RequiredArgsConstructor
public class JacksonMessageConverter implements MessageConverter {

    private final JsonMapper objectMapper;

    @Override
    public byte[] serialize(Object payload, MethodParameter parameter) {
        if (payload instanceof String str) {
            return str.getBytes(StandardCharsets.UTF_8);
        }
        if (payload instanceof byte[] buff) {
            return buff;
        }
        return objectMapper.writeValueAsBytes(payload);
    }

    @Override
    public Object deserialize(byte[] data, MethodParameter parameter) {
        if (data == null || data.length == 0) {
            return null;
        }
        Class<?> type = parameter.getParameter().getType();
        if (byte[].class == type) {
            return data;
        }
        String json = new String(data, StandardCharsets.UTF_8);
        if (String.class == type) {
            return json;
        }
        return parseObject(json, parameter.getGenericParameterType());
    }

    protected Object parseObject(String json, Type type) {
        return objectMapper.readValue(json, new tools.jackson.core.type.TypeReference<>() {
            @Override
            public Type getType() {
                return type;
            }
        });
    }


    @Override
    public String supportedType() {
        return MessageProtocol.JSON;
    }

}
