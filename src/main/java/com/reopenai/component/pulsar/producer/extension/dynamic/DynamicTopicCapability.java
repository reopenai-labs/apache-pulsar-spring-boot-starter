package com.reopenai.component.pulsar.producer.extension.dynamic;

import com.reopenai.component.pulsar.producer.core.capability.ProducerCapability;

/**
 * 动态Topic能力. 生产者接口继承此接口即拥有运行时动态添加Topic的能力,
 * 配合 {@link ProducerSelect} 参数可在发送时选择目标Topic.
 * <p>
 * 示例:
 * <pre>{@code
 * @PulsarProducer(tenant = "public", namespace = "default", topicName = "default-topic")
 * public interface MyProducer extends DynamicTopicCapability {
 *     @ProducerMethod
 *     void send(@ProducerSelect String topic, @MessageValue String message);
 * }
 * }</pre>
 *
 * @author Allen Huang
 */
public interface DynamicTopicCapability extends ProducerCapability {

    /**
     * 添加一个Topic(仅指定topicName, tenant/namespace使用生产者默认配置替换最后一段).
     *
     * @param alias    Topic别名, 用于 {@link ProducerSelect} 路由
     * @param topicName topic名称(不含tenant/namespace)
     */
    void addTopicByName(String alias, String topicName);

    /**
     * 添加一个完整Topic(包含 scheme://tenant/namespace/topicName).
     *
     * @param alias Topic别名, 用于 {@link ProducerSelect} 路由
     * @param topic 完整topic名称
     */
    void addTopicByFullName(String alias, String topic);

}
