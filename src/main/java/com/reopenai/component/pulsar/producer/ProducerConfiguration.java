package com.reopenai.component.pulsar.producer;

import com.reopenai.component.pulsar.enums.PersistentMode;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.springframework.beans.BeanUtils;

/**
 * @author Allen Huang
 */
@Getter
@RequiredArgsConstructor
@Builder(builderClassName = "Builder")
public class ProducerConfiguration {
    /**
     * 是否每次都创建新的producer实例
     */
    private final boolean prototype;
    /**
     * topic的模式，默认为持久化topic
     */
    private final PersistentMode persistentMode;
    /**
     * Topic的名称
     * Pulsar的topic格式为: tenant + namespace + topicName
     */
    private final String topic;
    /**
     * 生产者的名称
     */
    private final String producerName;

    /**
     * 生产者模式，默认为Shared模式
     */
    private final ProducerAccessMode accessMode;

    /**
     * 消息的压缩类型，默认为ZSTD.
     * 在使用序列化水平较高的消息格式例如protobuf时，可将压缩类型设置为{@link CompressionType#NONE}。
     * 因为在此类序列化中消息的大小本来就会被压缩的很小，如果继续压缩消息可能会导致消息的体积更大.
     */
    private final CompressionType compressionType;

    /**
     * 消息分区的hash算法类型，默认为Java字符串的hash算法
     */
    private final HashingScheme hashingScheme;

    /**
     * 生产者发送消息的超时时间，单位为毫秒，默认为五分钟
     */
    private final int sendTimeout;

    /**
     * 如果队列已经满了是否阻塞
     */
    private final boolean blockIfQueueFull;

    /**
     * 批量发送的最大消息数
     */
    private final int batchingMaxMessages;

    /**
     * 最大批量发送的大小
     */
    private final int batchingMaxBytes;

    /**
     * 是否启用批处理，默认不启用
     */
    private final boolean batchingEnabled;

    /**
     * 最大提交消息的时间，单位为毫秒
     */
    private final long batchingMaxPublishDelay;

    /**
     * 分区消息切换的时间，单位为毫秒
     */
    private final int roundRobinRouterBatchingPartitionSwitchFrequency;
    /**
     * builder实例
     */
    private final ProducerConfiguration.Builder builder;

    public ProducerConfiguration.Builder cloneBuilder() {
        Builder builder = builder();
        BeanUtils.copyProperties(this.builder, builder);
        return builder;
    }

}
