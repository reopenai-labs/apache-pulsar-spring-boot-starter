package com.reopenai.component.pulsar.producer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 用于生产者发送延迟消息。此框架支持两种延迟模式:
 * <pre>{@code
 *
 * //动态的延迟时间
 * @ProducerMethod
 * public void demo(@DelayedDelivery long delayedTime);
 *
 * //固定的延迟时间
 * @ProducerMethod(delayed = @DelayedDelivery(100))
 * public void demo();
 *
 * }</pre>
 *
 * @author Allen Huang
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface DelayedDelivery {

    /**
     * 消息的延迟时间，这个参数的值必须大于0
     */
    long value() default 0;

    /**
     * 时间单位，默认为毫秒
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}