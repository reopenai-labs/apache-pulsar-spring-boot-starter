package com.reopenai.component.pulsar.consumer.core.invoker;

import com.reopenai.component.pulsar.serialization.MessageConverter;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;

import java.util.Map;

/**
 * 消费者消息记录, 封装Pulsar {@link Message} 与用于反序列化的 {@link MessageConverter},
 * 为消费者参数解析提供统一的消息访问视图.
 * <p>
 * 与生产者的 {@code ProducerMessage} 对称: 生产者侧封装"待发送消息的构建", 消费者侧封装"已接收消息的读取".
 * {@code messageConverter} 由消费端根据消息属性中的序列化协议({@code MessageHeaders.PROTOCOL}, 生产者写入)
 * 或 {@code @ConsumerHandler.protocol} 选定.
 *
 * @author Allen Huang
 */
public record ConsumerMessageRecord(Message<byte[]> message, MessageConverter messageConverter) {

    public byte[] getData() {
        return message.getData();
    }

    public String getKey() {
        return message.getKey();
    }

    public MessageId getMessageId() {
        return message.getMessageId();
    }

    public String getTopicName() {
        return message.getTopicName();
    }

    public long getEventTime() {
        return message.getEventTime();
    }

    public long getPublishTime() {
        return message.getPublishTime();
    }

    public Map<String, String> getProperties() {
        return message.getProperties();
    }

    public String getProperty(String name) {
        return message.getProperty(name);
    }
}
