package com.reopenai.component.pulsar.consumer.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 消费者实例注册器: 集中管理框架创建的所有 {@link Consumer} 的生命周期, 容器关闭时统一关闭.
 * 与 producer 端 {@code ProducerRegistry} 对称.
 *
 * @author Allen Huang
 */
@Slf4j
public class ConsumerRegistry implements DisposableBean {

    private final CopyOnWriteArrayList<Consumer<byte[]>> consumers = new CopyOnWriteArrayList<>();

    /**
     * 注册一个已创建的消费者, 交由本注册器管理生命周期.
     *
     * @param consumer 已创建的消费者实例
     */
    public void register(Consumer<byte[]> consumer) {
        consumers.add(consumer);
    }

    @Override
    public void destroy() {
        for (Consumer<byte[]> consumer : consumers) {
            try {
                consumer.close();
            } catch (PulsarClientException e) {
                log.error("[Pulsar][Consumer]关闭消费者失败.topic={}", consumer.getTopic(), e);
            }
        }
    }

}
