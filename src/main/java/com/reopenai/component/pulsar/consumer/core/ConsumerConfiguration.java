package com.reopenai.component.pulsar.consumer.core;

import com.reopenai.component.pulsar.annotation.BatchReceivePolicyConfig;
import com.reopenai.component.pulsar.annotation.DeadLetterPolicyConfig;
import com.reopenai.component.pulsar.annotation.PulsarConsumer;
import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.BatchReceivePolicy;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 消费者配置器: 将 {@link PulsarConsumer} 注解配置映射到 Pulsar {@link ConsumerBuilder}.
 * <p>
 * 抽取自 {@code PulsarConsumerBeanPostProcessor}, 使配置逻辑可独立测试与复用,
 * 与 producer 端 {@code ProducerConfiguration} 形成对称.
 *
 * @author Allen Huang
 */
@RequiredArgsConstructor
public class ConsumerConfiguration {

    private final Environment environment;

    /**
     * 将 {@link PulsarConsumer} 注解的配置应用到 {@link ConsumerBuilder}.
     *
     * @param builder    Pulsar 消费者构建器
     * @param annotation 消费者注解配置
     */
    public void configure(ConsumerBuilder<byte[]> builder, PulsarConsumer annotation) {
        configureTopics(builder, annotation);
        builder.subscriptionName(environment.resolvePlaceholders(annotation.subscriptionName()))
                .subscriptionType(annotation.subscriptionType())
                .subscriptionMode(annotation.subscriptionMode())
                .subscriptionInitialPosition(annotation.subscriptionInitialPosition())
                .receiverQueueSize(annotation.receiverQueueSize())
                .maxTotalReceiverQueueSizeAcrossPartitions(annotation.maxTotalReceiverQueueSizeAcrossPartitions())
                .acknowledgmentGroupTime(annotation.acknowledgmentGroupTime(), TimeUnit.MILLISECONDS)
                .enableBatchIndexAcknowledgment(annotation.enableBatchIndexAcknowledgment());
        if (annotation.enableRetry()) {
            builder.enableRetry(true);
        }
        builder.negativeAckRedeliveryDelay(annotation.negativeAckRedeliveryDelay(), TimeUnit.MILLISECONDS);
        if (StringUtils.hasText(annotation.consumerName())) {
            builder.consumerName(environment.resolvePlaceholders(annotation.consumerName()));
        }
        configureDeadLetter(builder, annotation.deadLetterPolicy());
        if (annotation.enableBatch()) {
            configureBatchReceive(builder, annotation.batchReceivePolicy());
        }
    }

    private void configureTopics(ConsumerBuilder<byte[]> builder, PulsarConsumer annotation) {
        String[] topics = annotation.topicNames();
        if (topics.length > 0) {
            List<String> resolved = new ArrayList<>(topics.length);
            for (String topic : topics) {
                resolved.add(environment.resolvePlaceholders(topic));
            }
            builder.topics(resolved);
        }
        if (StringUtils.hasText(annotation.topicsPattern())) {
            builder.topicsPattern(environment.resolvePlaceholders(annotation.topicsPattern()));
        }
    }

    private void configureDeadLetter(ConsumerBuilder<byte[]> builder, DeadLetterPolicyConfig config) {
        if (!config.enable()) {
            return;
        }
        var dlqBuilder = DeadLetterPolicy.builder()
                .maxRedeliverCount(config.maxRedeliverCount());
        if (StringUtils.hasText(config.deadLetterTopic())) {
            dlqBuilder.deadLetterTopic(environment.resolvePlaceholders(config.deadLetterTopic()));
        }
        if (StringUtils.hasText(config.retryTopic())) {
            dlqBuilder.retryLetterTopic(environment.resolvePlaceholders(config.retryTopic()));
        }
        builder.deadLetterPolicy(dlqBuilder.build());
    }

    private void configureBatchReceive(ConsumerBuilder<byte[]> builder, BatchReceivePolicyConfig config) {
        builder.batchReceivePolicy(
                new BatchReceivePolicy.Builder()
                        .maxNumMessages(config.maxNumMessages())
                        .maxNumBytes(config.maxNumBytes())
                        .timeout(config.timeout(), TimeUnit.MILLISECONDS)
                        .build());
    }

}
