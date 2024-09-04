package com.reopenai.component.pulsar.producer.core.invoker;

import com.reopenai.component.pulsar.serialization.MessageConverter;
import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.springframework.core.MethodParameter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Producer消息, 封装消息构建与序列化能力, 屏蔽底层{@link TypedMessageBuilder}的细节
 *
 * @author Allen Huang
 */
@RequiredArgsConstructor
public class ProducerMessage {

    private final MessageConverter converter;

    private final TypedMessageBuilder<byte[]> builder;

    /**
     * 设置消息荷载, 使用绑定的序列化器序列化后写入
     */
    public void value(MethodParameter parameter, Object message) {
        byte[] payload = converter.serialize(message, parameter);
        builder.value(payload);
    }

    public void key(String key) {
        builder.key(key);
    }

    public void property(String name, String value) {
        builder.property(name, value);
    }

    public void properties(Map<String, String> properties) {
        builder.properties(properties);
    }

    public void eventTime(long timestamp) {
        builder.eventTime(timestamp);
    }

    public void deliverAfter(long delay, TimeUnit unit) {
        builder.deliverAfter(delay, unit);
    }

    public MessageId send() throws PulsarClientException {
        return builder.send();
    }

    public CompletableFuture<MessageId> sendAsync() {
        return builder.sendAsync();
    }

}
