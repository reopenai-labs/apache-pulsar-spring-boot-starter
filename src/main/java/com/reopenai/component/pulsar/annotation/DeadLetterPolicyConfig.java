package com.reopenai.component.pulsar.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于Pulsar消费者的死信队列配置
 *
 * @author Allen Huang
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DeadLetterPolicyConfig {

    /**
     * 是否开启死信队列，默认为开启状态.
     */
    boolean enable() default true;

    /**
     * 消费失败时的最大重试次数。默认为8次
     */
    int maxRedeliverCount() default 8;

    /**
     * 死信队列的名称
     */
    String deadLetterTopic() default "persistent://REOPENAI/SYSTEM/GLOBAL_DEFAULT_DLQ";

    /**
     * 重试队列的名称
     */
    String retryTopic() default "";

}