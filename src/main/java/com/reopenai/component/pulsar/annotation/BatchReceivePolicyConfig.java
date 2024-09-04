package com.reopenai.component.pulsar.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 批量接收消息的策略配置
 *
 * @author Allen Huang
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface BatchReceivePolicyConfig {
    /**
     * 达到最大消息时触发消费
     */
    int maxNumMessages() default 100;

    /**
     * 达到最大字节数时触发消费
     */
    int maxNumBytes() default 1024 * 1024;

    /**
     * 达到最大超时时间时触发消费，单位为毫秒
     */
    int timeout() default 200;

}
