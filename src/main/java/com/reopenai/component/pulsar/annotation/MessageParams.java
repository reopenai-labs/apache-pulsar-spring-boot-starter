package com.reopenai.component.pulsar.annotation;

import java.lang.annotation.*;

/**
 * 作用在消费者参数的注解上，表示要获取消息中的全部属性。
 * 被标记的参数必须是 {@link java.util.Map} 类型。
 *
 * @author Allen Huang
 */
@Inherited
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageParams {
}
