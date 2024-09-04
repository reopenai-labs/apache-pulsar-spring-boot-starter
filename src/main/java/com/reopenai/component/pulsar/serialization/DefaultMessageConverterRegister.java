package com.reopenai.component.pulsar.serialization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Allen Huang
 */
public class DefaultMessageConverterRegister implements MessageConverterRegister {

    private final Map<String, MessageConverter> converters;

    public DefaultMessageConverterRegister(List<MessageConverter> converters) {
        this.converters = new HashMap<>();
        for (MessageConverter converter : converters) {
            this.register(converter);
        }
    }

    @Override
    public synchronized void register(MessageConverter converter) {
        String protocol = converter.supportType();
        MessageConverter instance = this.converters.get(protocol);
        if (instance != null) {
            throw new IllegalArgumentException("同一种序列化协议存在多个消息转换器.type=" + protocol
                    + ",instance1=" + instance.getClass().getName() + ",instance2=" + converter.getClass().getName());
        }
        this.converters.put(protocol, converter);
    }

    @Override
    public MessageConverter get(String protocol) {
        MessageConverter converter = this.converters.get(protocol);
        if (converter == null) {
            throw new IllegalArgumentException("不支持此序列化协议.如果想要扩展序列化协议，请通过实现MessageConverter接口达成.protocol=" + protocol);
        }
        return converter;
    }

}
