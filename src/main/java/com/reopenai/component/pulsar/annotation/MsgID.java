package com.reopenai.component.pulsar.annotation;

import org.apache.pulsar.client.api.MessageId;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记在消费者的方法上，表明消息ID.被标记的字段必须是{@link  MessageId}类型
 *
 * @author Allen Huang
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MsgID {
}
