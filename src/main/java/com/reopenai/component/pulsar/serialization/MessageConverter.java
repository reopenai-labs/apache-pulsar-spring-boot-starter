package com.reopenai.component.pulsar.serialization;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;

/**
 * 消息转换器，用于处理消息的序列化和反序列化
 *
 * @author Allen Huang
 * @since 1.0.0
 */
public interface MessageConverter extends Ordered {
    /**
     * 序列化消息
     *
     * @param message       The content of the message that needs to be serialized
     * @param parameterInfo Raw type information for this message
     * @return Serialized byte array
     */
    byte[] serializer(Object message, MethodParameter parameterInfo);

    /**
     * 反序列化消息
     *
     * @param message       The content of the message that needs to be serialized
     * @param parameterInfo Raw type information for this message
     * @return Deserialized object instance
     */
    Object deserializer(byte[] message, MethodParameter parameterInfo);

    /**
     * 此消息转换器所支持的类型
     */
    String supportType();

    @Override
    default int getOrder() {
        return 0;
    }

}