package com.reopenai.component.pulsar.serialization;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Allen Huang
 */
public class MessageConverterRegistry {

    private final Map<String, MessageConverter> converters;

    public MessageConverterRegistry(List<MessageConverter> converters) {
        this.converters = new ConcurrentHashMap<>();
        for (MessageConverter converter : converters) {
            this.register(converter);
        }
    }

    public void register(MessageConverter converter) {
        String protocol = converter.supportedType();
        MessageConverter existing = this.converters.putIfAbsent(protocol, converter);
        if (existing != null) {
            throw new IllegalArgumentException("同一种序列化协议存在多个消息转换器.type=" + protocol
                    + ",instance1=" + existing.getClass().getName() + ",instance2=" + converter.getClass().getName());
        }
    }

    public MessageConverter get(String protocol) {
        MessageConverter converter = this.converters.get(protocol);
        if (converter == null) {
            throw new IllegalArgumentException("不支持此序列化协议.如果想要扩展序列化协议，请通过实现MessageConverter接口达成.protocol=" + protocol);
        }
        return converter;
    }

}
