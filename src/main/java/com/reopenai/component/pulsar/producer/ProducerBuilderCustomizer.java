package com.reopenai.component.pulsar.producer;

import org.apache.pulsar.client.api.ProducerBuilder;

/**
 * ProducerBuilder扩展器,在初始化ProducerBuilder实例后被调用.
 *
 * @author Allen Huang
 */
public interface ProducerBuilderCustomizer {

    /**
     * 自定义ProducerBuilder的配置
     *
     * @param builder ProducerBuilder实例
     */
    void customize(ProducerBuilder<byte[]> builder);

}
