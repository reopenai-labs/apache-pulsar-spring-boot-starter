package com.reopenai.component.pulsar.producer.register;

import com.reopenai.component.pulsar.producer.ProducerBuilderCustomizer;
import com.reopenai.component.pulsar.producer.ProducerConfiguration;
import com.reopenai.component.pulsar.producer.ProducerCustomizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * ProducerRegister的默认实现
 *
 * @author Allen Huang
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultProducerRegister implements ProducerRegister, EnvironmentAware {

    protected Environment environment;

    protected final PulsarClient pulsarClient;

    protected final ObjectProvider<ProducerCustomizer> producerCustomizers;

    protected final ObjectProvider<ProducerBuilderCustomizer> builderCustomizers;

    protected final Map<String, Producer<byte[]>> producers = new HashMap<>();

    protected final Set<Producer<byte[]>> prototypeProducers = new HashSet<>();

    @Override
    public Producer<byte[]> getInstance(ProducerConfiguration config) throws PulsarClientException {
        if (config.isPrototype()) {
            synchronized (this.prototypeProducers) {
                Producer<byte[]> producer = createProducer(config);
                this.prototypeProducers.add(producer);
                return producer;
            }
        }
        // 单例方式实现
        String topic = config.getTopic();
        Producer<byte[]> producer = producers.get(topic);
        if (producer == null) {
            synchronized (this.producers) {
                producer = producers.get(topic);
                if (producer == null) {
                    producer = createProducer(config);
                    producers.put(topic, producer);
                }
            }
        }
        return producer;
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
        builder.properties(Map.of("application", applicationName));
        // 扩展定制
        builderCustomizers.orderedStream().
                forEach(customizer -> customizer.customize(builder));
        Producer<byte[]> instance = builder.create();
        for (ProducerCustomizer customizer : producerCustomizers) {
            instance = customizer.customize(instance);
        }
        return instance;
    }

    @Override
    public void destroy() throws Exception {
        for (Map.Entry<String, Producer<byte[]>> entry : producers.entrySet()) {
            Producer<byte[]> producer = entry.getValue();
            try {
                producer.close();
            } catch (Throwable t) {
                log.error("关闭生产者出错.topic={}", producer.getTopic(), t);
            }
        }
        for (Producer<byte[]> producer : prototypeProducers) {
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
