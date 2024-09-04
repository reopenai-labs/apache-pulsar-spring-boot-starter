package com.reopenai.component.pulsar.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reopenai.component.pulsar.constant.MessageProtocol;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * 基于Jackson实现的JSON序列化解析器
 *
 * @author Allen Huang
 */
@RequiredArgsConstructor
public class JacksonMessageConverter implements MessageConverter {

    private final ObjectMapper objectMapper;

    @Override
    public byte[] serializer(Object message, MethodParameter parameterInfo) {
        if (message instanceof String str) {
            return str.getBytes(StandardCharsets.UTF_8);
        }
        if (message instanceof byte[] buff) {
            return buff;
        }
        return toJSONString(message).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Object deserializer(byte[] message, MethodParameter parameterInfo) {
        Parameter parameter = parameterInfo.getParameter();
        Class<?> type = parameter.getType();
        if (byte[].class == type) {
            return message;
        }
        String json = new String(message, StandardCharsets.UTF_8);
        if (String.class == type) {
            return json;
        }
        return parseObject(json, parameter.getParameterizedType());
    }

    protected Object parseObject(String json, Type type) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
                @Override
                public Type getType() {
                    return type;
                }
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String toJSONString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException(error);
        }
    }

    @Override
    public String supportType() {
        return MessageProtocol.JSON;
    }

}