package com.reopenai.component.pulsar.producer.register;

import com.reopenai.component.pulsar.producer.ProducerConfiguration;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.DisposableBean;

/**
 * Producer实例注册器,负责根据Producer配置信息创建Producer实例.
 * 被创建的Producer实例的声明周期将会被Spring管理.当Spring容器被关闭时,也会关闭所有创建的Producer实例.
 *
 * @author Allen Huang
 */
public interface ProducerRegister extends DisposableBean {

    /**
     * 创建producer实例.
     *
     * @param config 配置内容
     * @return producer实例
     */
    Producer<byte[]> getInstance(ProducerConfiguration config) throws PulsarClientException;

}
