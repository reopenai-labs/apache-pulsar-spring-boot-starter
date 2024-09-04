package com.reopenai.component.pulsar.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消费者方法参数注解, 注入 Pulsar 原始消息 ID. 被标记的参数必须是
 * {@link org.apache.pulsar.client.api.MessageId} 类型.
 * <p>
 * 命名为 {@code Raw} 以与 {@code MessageId} 类型本身区分.
 *
 * @author Allen Huang
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RawMessageId {
}
