package com.reopenai.component.pulsar.producer.extension.dynamic;

/**
 * 能够在运行时动态添加producer的能力
 *
 * @author Allen Huang
 */
public interface DynamicProducerAbility {

    /**
     * 动态添加一个topic
     *
     * @param alias     producer别名
     * @param topicName 不包含tenant、namespace的topicName.tenant和namespace将会使用默认的填充
     */
    void addTopicByName(String alias, String topicName);

    /**
     * 动态添加一个topic
     *
     * @param alias producer别名
     * @param topic 包含tenant、namespace、topicName的完成topic
     */
    void addTopicByFullName(String alias, String topic);

}
