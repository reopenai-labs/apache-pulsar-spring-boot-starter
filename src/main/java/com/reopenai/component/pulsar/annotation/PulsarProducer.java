package com.reopenai.component.pulsar.annotation;

import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.ProducerAccessMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Allen Huang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PulsarProducer {

    /**
     * topic的模式，默认为持久化topic
     */
    boolean persistentMode() default true;

    /**
     * 生产者的 namespace, 默认 {@code default}. 支持通过 {@code spring.pulsar.global.namespace} 全局覆盖.
     */
    String namespace() default "${spring.pulsar.global.namespace:default}";

    /**
     * 生产者的租户.
     * 如果没有配置该值，那么将使用全局配置中配置的tenant.
     * 如果全局配置中也没有配置tenant，那么将使用public作为默认的tenant.
     */
    String tenant() default "${spring.pulsar.global.tenant:public}";

    /**
     * Topic的名称，支持SPEL表达式.
     * Pulsar的topic格式为: tenant + namespace + topicName
     */
    String topicName();

    /**
     * 生产者的名称
     */
    String producerName() default "";

    /**
     * 生产者模式，默认为Shared模式
     */
    ProducerAccessMode accessMode() default ProducerAccessMode.Shared;

    /**
     * 消息的压缩类型，默认为ZSTD.
     * 在使用序列化水平较高的消息格式例如protobuf时，可将压缩类型设置为{@link CompressionType#NONE}。
     * 因为在此类序列化中消息的大小本来就会被压缩的很小，如果继续压缩消息可能会导致消息的体积更大.
     */
    CompressionType compressionType() default CompressionType.ZSTD;

    /**
     * 消息分区的hash算法类型，默认为Java字符串的hash算法
     */
    HashingScheme hashingScheme() default HashingScheme.JavaStringHash;

    /**
     * 生产者发送消息的超时时间，单位为毫秒，默认为五分钟
     */
    int sendTimeout() default 300000;

    /**
     * 如果队列已经满了是否阻塞
     */
    boolean blockIfQueueFull() default true;

    /**
     * 批处理配置
     */
    ProducerBatchPolicy batch() default @ProducerBatchPolicy;

}
