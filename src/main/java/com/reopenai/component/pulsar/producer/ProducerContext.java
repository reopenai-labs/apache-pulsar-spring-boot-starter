package com.reopenai.component.pulsar.producer;

import lombok.Getter;
import org.apache.pulsar.client.api.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Producer上下文
 *
 * @author Allen Huang
 */
@Getter
public class ProducerContext {

    /**
     * topicName
     */
    private final String topic;

    /**
     * 接口对象的真实类型
     */
    private final Class<?> type;

    /**
     * 配置信息
     */
    private final ProducerConfiguration configuration;

    /**
     * producer实例
     */
    private final Producer<byte[]> producer;

    /**
     * 扩展属性
     */
    private final Map<String, Object> attributes;

    public ProducerContext(Class<?> type, Producer<byte[]> producer, ProducerConfiguration configuration) {
        this.type = type;
        this.producer = producer;
        this.attributes = new HashMap<>();
        this.topic = producer.getTopic();
        this.configuration = configuration;
    }

    public void addAttributes(String key, Object value) {
        synchronized (this.attributes) {
            if (attributes.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate key: " + key + " in ProducerContext.topic: " + topic);
            }
            attributes.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttributes(String key) {
        Object value = this.attributes.get(key);
        if (value != null) {
            return (T) value;
        }
        return null;
    }

    public Logger getLogger() {
        return LoggerFactory.getLogger(this.type);
    }

}
