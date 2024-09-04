package com.reopenai.component.pulsar.producer.annotation;

import com.reopenai.component.pulsar.annotation.MessageProperty;
import com.reopenai.component.pulsar.constant.MessageProtocol;

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
     * // delayedTime 动态控制延迟的时间
     * void send(@MessageValue Object message,@DelayedDelivery long delayedTime)
     *
     * }</pre>
     */
    DelayedDelivery delayed() default @DelayedDelivery;

}
