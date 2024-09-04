package com.reopenai.component.pulsar.serialization;

import org.springframework.core.MethodParameter;

/**
 * 消息转换器,用于处理消息的序列化和反序列化
 *
 * @author Allen Huang
 * @since 1.0.0
 */
public interface MessageConverter {
    /**
     * 序列化消息荷载
     *
     * @param payload       待序列化的消息荷载
     * @param parameter 消息参数的类型信息
     * @return 序列化后的字节数组
     */
    byte[] serialize(Object payload, MethodParameter parameter);

    /**
     * 反序列化消息荷载
     *
     * @param data          待反序列化的字节数据
     * @param parameter 消息参数的类型信息
     * @return 反序列化后的对象
     */
    Object deserialize(byte[] data, MethodParameter parameter);

    /**
     * 此消息转换器所支持的类型
     */
    String supportedType();

}
