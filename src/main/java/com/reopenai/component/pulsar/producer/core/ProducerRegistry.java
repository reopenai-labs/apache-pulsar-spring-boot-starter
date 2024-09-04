package com.reopenai.component.pulsar.producer.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Producer实例注册器,负责根据Producer配置信息创建Producer实例.
 * <p>
 * 相同{@link ProducerConfiguration}(topic + producerName + accessMode + 压缩/批处理等全部参数)的Producer
 * 实例会被复用, 其生命周期由本注册器管理, 容器关闭时统一关闭.
 *
 * @author Allen Huang
 */
@Slf4j
@RequiredArgsConstructor
public class ProducerRegistry implements EnvironmentAware, DisposableBean {

    protected Environment environment;

    protected final PulsarClient pulsarClient;

    protected final ObjectProvider<ProducerBuilderCustomizer<byte[]>> builderCustomizers;

    /**
     * Producer 内置属性 key: 标记消息来源应用名(取自 {@code spring.application.name})
     */
    private static final String APPLICATION_PROPERTY_KEY = "application";

    /**
     * 以完整的 {@link ProducerConfiguration} 作为缓存键, 确保不同配置(topic/producerName/accessMode/压缩/批处理等)
     * 得到各自独立的 Producer 实例, 避免同 topic 不同配置互相覆盖.
     */
    protected final Map<ProducerConfiguration, Producer<byte[]>> producers = new ConcurrentHashMap<>();

    /**
     * 获取(必要时创建)指定配置对应的 Producer 实例. 相同 {@link ProducerConfiguration} 的 Producer 全局复用.
     * <p>
     * 线程安全: 并发首次创建同一配置时可能短暂创建多个实例, 多余的会被关闭丢弃, 仅保留首个注册的.
     *
     * @param config Producer 配置(作为复用键)
     * @return 复用或新建的 Producer 实例
     */
    public Producer<byte[]> getProducer(ProducerConfiguration config) throws PulsarClientException {
        Producer<byte[]> producer = producers.get(config);
        if (producer != null) {
            return producer;
        }
        Producer<byte[]> created = createProducer(config);
        Producer<byte[]> existing = producers.putIfAbsent(config, created);
        if (existing != null) {
            try {
                created.close();
            } catch (PulsarClientException e) {
                log.error("关闭重复创建的Producer出错.topic={}", created.getTopic(), e);
            }
            return existing;
        }
        return created;
    }

    protected Producer<byte[]> createProducer(ProducerConfiguration configuration) throws PulsarClientException {
        ProducerBuilder<byte[]> builder = pulsarClient.newProducer(Schema.BYTES)
                .topic(configuration.getTopic())
                .accessMode(configuration.getAccessMode())
                .hashingScheme(configuration.getHashingScheme())
                .compressionType(configuration.getCompressionType())
                .blockIfQueueFull(configuration.isBlockIfQueueFull())
                .sendTimeout(configuration.getSendTimeout(), TimeUnit.MILLISECONDS);

        String producerName = configuration.getProducerName();
        if (StringUtils.hasText(producerName)) {
            builder.producerName(producerName);
        }
        if (configuration.isBatchingEnabled()) {
            builder.batcherBuilder(BatcherBuilder.KEY_BASED)
                    .batchingMaxBytes(configuration.getBatchingMaxBytes())
                    .batchingMaxMessages(configuration.getBatchingMaxMessages())
                    .batchingMaxPublishDelay(configuration.getBatchingMaxPublishDelay(), TimeUnit.MILLISECONDS)
                    .roundRobinRouterBatchingPartitionSwitchFrequency(configuration.getRoundRobinRouterBatchingPartitionSwitchFrequency());
        }
        String applicationName = this.environment.resolvePlaceholders("${spring.application.name:unknown}");
        builder.properties(Map.of(APPLICATION_PROPERTY_KEY, applicationName));
        // 扩展定制
        builderCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.create();
    }

    @Override
    public void destroy() throws Exception {
        for (Producer<byte[]> producer : producers.values()) {
            try {
                producer.close();
            } catch (Throwable t) {
                log.error("关闭生产者出错.topic={}", producer.getTopic(), t);
            }
        }
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }
}
