package com.reopenai.component.pulsar.annotation;

import java.lang.annotation.*;

/**
 * 定义在生产者参数中时，表明要传递的参数为EventTime.
 * 定义在消费者参数中时，表明要获取的值为EventTime.
 * 被标记的字段必须是long类型
 *
 * @author Allen Huang
 */
@Inherited
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageEventTime {
}
