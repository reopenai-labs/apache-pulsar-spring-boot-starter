package com.reopenai.component.pulsar.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在生产者或消费者有多个参数时，用来表明某参数为消息实体。例如:
 * <pre>{@code
 * //生产者中定义了多个参数，需要显示的指定消息实体
 * @ProducerMethod
 * public MessageId producerDemo(@MessageKey String key, @MessageValue MessageDTO message){
 *
 * }
 *
 * //消费者中定义了多个参数，需要显示的指定消息的实体
 * @ConsumerMethod
 * public void consumerDemo(@MessageKey String key, @MessageValue MessageDTO message){
 *
 * }
 *
 * }</pre>
 *
 * @author Allen Huang
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageValue {
}
