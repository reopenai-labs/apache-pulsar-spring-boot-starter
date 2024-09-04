package com.reopenai.component.pulsar.annotation;

import com.reopenai.component.pulsar.producer.core.TransactionPhase;
import com.reopenai.component.pulsar.serialization.MessageProtocol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Allen Huang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProducerMethod {
    /**
     * 发送消息时使用的序列化协议，默认为JSON
     */
    String protocol() default MessageProtocol.JSON;
    /**
     * 消息的KEY.具有相同消息的key能保证消息的有序性。
     * 消费者使用key_shared的模式消费消息能够保证消息有序的被同一个消费者消费。
     */
    String messageKey() default "";
    /**
     * 属性列表。如果配置了该属性，在发送消息时会讲列表中的属性写入到消息中
     */
    MessageProperty[] properties() default {};
    /**
     * 固定的延迟策略.如果想要动态的控制投递延迟消息，可使用以下方式:<pre>{@code
     *
     * // deliveryDelayMillis 动态控制延迟的时间
     * void send(@MessageValue Object message,@DelayedDelivery long deliveryDelayMillis)
     *
     * }</pre>
     */
    DelayedDelivery delayed() default @DelayedDelivery;

    /**
     * 是否自动生成消息唯一键(hostname+uuid), 写入消息属性, 供消费端去重/追踪. 默认关闭.
     */
    boolean autoUniqueKey() default false;

    /**
     * 事务感知发送策略. 默认 {@link TransactionPhase#IMMEDIATE}(立即发送, 不感知事务).
     * 设为 {@link TransactionPhase#AFTER_COMMIT} 时, 仅在Spring事务(@Transactional)成功提交后才发送,
     * 事务回滚则不发送, 保证DB与消息一致性.
     */
    TransactionPhase transaction() default TransactionPhase.IMMEDIATE;

}
