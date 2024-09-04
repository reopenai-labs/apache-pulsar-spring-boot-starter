package com.reopenai.component.pulsar.annotation;

import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个类为Pulsar的消费者，并会将这个类注册到Spring容器中。
 * 在Spring容器启动的过程中会解析消费者中的所有方法。
 * 如果方法包含{@link ConsumerHandler}注解，那么这个方法会被识别为Pulsar的消息处理器。
 *
 * @author Allen Huang
 */
@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PulsarConsumer {

    /**
     * Topic的名称，支持多topic订阅.
     * Pulsar的topic格式为: tenant + namespace + topicName。
     * topicNames和topicsPattern不能同时为空.
     */
    String[] topicNames() default {};

    /**
     * Topic的正则表达式.
     * topicNames和topicsPattern不能同时为空.
     * 如果同时存在，则会使用topicNames作为topic的名称.
     */
    String topicsPattern() default "";

    /**
     * 消费者的订阅名称，默认为spring.application.name
     */
    String subscriptionName() default "${spring.application.name}";

    /**
     * 消费者的订阅类型。默认为shared模式
     */
    SubscriptionType subscriptionType() default SubscriptionType.Shared;

    /**
     * 订阅模式.
     * <ul>
     *     <li>SubscriptionMode.Durable: 订阅会持久化保存游标.</li>
     *     <li>SubscriptionMode.NonDurable: 轻量化订阅.当进程关闭后不保存游标信息.</li>
     * </ul>
     * 此参数可以配合subscriptionType完成Reader的功能.
     * <p>
     * 例如:<pre>{@code
     * @PulsarConsumer(
     *      subscriptionType = SubscriptionType.Exclusive,
     *      subscriptionMode = SubscriptionMode.NonDurable
     * )
     * public class Consumer {
     * }
     * }</pre>
     * 你会得到一个不受其他消费者干扰的消费者，并且当此消费者关闭之后，订阅也会被自动释放，不会形成阻塞的消息.
     */
    SubscriptionMode subscriptionMode() default SubscriptionMode.Durable;

    /**
     * 消费者的名称，默认未设置名称
     */
    String consumerName() default "";

    /**
     * Pulsar每一次向服务端拉取的消息条目数，默认为50，
     */
    int receiverQueueSize() default 50;

    /**
     * 跨分区接收队列的最大大小
     */
    int maxTotalReceiverQueueSizeAcrossPartitions() default 50000;

    /**
     * 消费者消费消息的初始化offset。默认从最消息最新未被消费的offset处开始订阅
     */
    SubscriptionInitialPosition subscriptionInitialPosition() default SubscriptionInitialPosition.Latest;

    /**
     * 是否开启消息的批量ACK。
     * 要想启用此功能，必须在broker配置允许批量ACK
     */
    boolean enableBatchIndexAcknowledgment() default false;

    /**
     * 在消费组内ack的时间，单位为毫秒。默认100毫秒
     */
    long acknowledgmentGroupTime() default 100;

    /**
     * 当消费者消费失败时，是否允许重试。
     */
    boolean enableRetry() default true;

    /**
     * 死信队列配置
     */
    DeadLetterPolicyConfig deadLetterPolicy() default @DeadLetterPolicyConfig;

    //--------------------------------
    //          批量消费
    //--------------------------------

    /**
     * 是否开启批处理.
     */
    boolean enableBatch() default false;

    /**
     * 批处理配置
     */
    BatchReceivePolicyConfig batchReceivePolicy() default @BatchReceivePolicyConfig;

    /**
     * 设置重新发送处理失败的消息之前等待的延迟时间。单位为毫秒,默认值为1分钟。
     * 当调用Consumer.negativeAcknowledge(Message)时，失败的消息将在固定超时后重新传递。
     */
    long negativeAckRedeliveryDelay() default 60000;

}