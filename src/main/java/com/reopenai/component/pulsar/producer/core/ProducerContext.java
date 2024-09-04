package com.reopenai.component.pulsar.producer.core;

import lombok.Getter;
import org.apache.pulsar.client.api.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Producer上下文
 *
 * @author Allen Huang
 */
@Getter
public class ProducerContext {

    /**
     * 完整 Topic URL (scheme://tenant/namespace/topicName)
     */
    private final String topic;

    /**
     * 声明式 Producer 接口的真实类型
     */
    private final Class<?> producerType;

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

    public ProducerContext(Class<?> producerType, Producer<byte[]> producer, ProducerConfiguration configuration) {
        this.producerType = producerType;
        this.producer = producer;
        this.attributes = new ConcurrentHashMap<>();
        this.topic = producer.getTopic();
        this.configuration = configuration;
    }

    public void addAttribute(String key, Object value) {
        if (attributes.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate key: " + key + " in ProducerContext.topic: " + topic);
        }
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        Object value = this.attributes.get(key);
        if (value != null) {
            return (T) value;
        }
        return null;
    }

    public Logger getLogger() {
        return LoggerFactory.getLogger(this.producerType);
    }

}
