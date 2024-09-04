package com.reopenai.component.pulsar.producer.invoker;

import com.reopenai.component.pulsar.serialization.MessageConverter;
import lombok.AllArgsConstructor;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.springframework.core.MethodParameter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Producer消息
 *
 * @author Allen Huang
 */
@AllArgsConstructor
public class ProducerMessage implements TypedMessageBuilder<byte[]> {

    private final MessageConverter converter;

    private TypedMessageBuilder<byte[]> builder;

    @Override
    public MessageId send() throws PulsarClientException {
        return builder.send();
    }

    @Override
    public CompletableFuture<MessageId> sendAsync() {
        return builder.sendAsync();
    }

    @Override
    public TypedMessageBuilder<byte[]> key(String key) {
        builder.key(key);
        return this;
    }

    @Override
    public TypedMessageBuilder<byte[]> keyBytes(byte[] key) {
        builder.keyBytes(key);
        return this;
    }

    @Override
    public TypedMessageBuilder<byte[]> orderingKey(byte[] orderingKey) {
        builder.orderingKey(orderingKey);
        return this;
    }

    public TypedMessageBuilder<byte[]> value(MethodParameter parameterInfo, Object object) {
        byte[] payload = converter.serializer(object, parameterInfo);
        return value(payload);
    }

    @Override
    public TypedMessageBuilder<byte[]> value(byte[] value) {
        builder.value(value);
        return this;
    }

    @Override
    public TypedMessageBuilder<byte[]> property(String name, String value) {
        builder.property(name, value);
        return this;
    }

    @Override
    public TypedMessageBuilder<byte[]> properties(Map<String, String> properties) {
        builder.properties(properties);
        return this;
    }

    @Override
    public TypedMessageBuilder<byte[]> eventTime(long timestamp) {
        builder.eventTime(timestamp);
        return this;
    }

    @Override
    public TypedMessageBuilder<byte[]> sequenceId(long sequenceId) {
        builder.sequenceId(sequenceId);
        return this;
    }

    @Override
    public TypedMessageBuilder<byte[]> replicationClusters(List<String> clusters) {
        builder.replicationClusters(clusters);
        return this;
    }

    @Override
    public TypedMessageBuilder<byte[]> disableReplication() {
        builder.disableReplication();
        return this;
    }

    @Override
    public TypedMessageBuilder<byte[]> deliverAt(long timestamp) {
        builder.deliverAt(timestamp);
        return this;
    }

    @Override
    public TypedMessageBuilder<byte[]> deliverAfter(long delay, TimeUnit unit) {
        builder.deliverAfter(delay, unit);
        return this;
    }

    @Override
    public TypedMessageBuilder<byte[]> loadConf(Map<String, Object> config) {
        builder.loadConf(config);
        return this;
    }

}
