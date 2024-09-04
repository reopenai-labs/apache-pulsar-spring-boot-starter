package com.reopenai.component.pulsar.producer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Allen Huang
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProducerBatchPolicy {
    /**
     * 是否启用批处理，默认不启用
     */
    boolean enables() default false;
    /**
     * 最大批量发送的大小
     */
    int batchingMaxBytes() default 128 * 1024 * 1024;

    /**
     * 批量发送的最大消息数
     */
    int batchingMaxMessages() default 10000;

    /**
     * 最大提交消息的时间，单位为毫秒
     */
    long batchingMaxPublishDelay() default 1L;

    /**
     * 分区消息切换的时间，单位为毫秒
     */
    int roundRobinRouterBatchingPartitionSwitchFrequency() default 2;

}
