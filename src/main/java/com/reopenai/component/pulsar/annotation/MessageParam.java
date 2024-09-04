package com.reopenai.component.pulsar.annotation;

import java.lang.annotation.*;

/**
 * 标记一个参数为消息的属性。
 * 被标记的字段必须是{@link java.lang.String}类型
 *
 * @author Allen Huang
 */
@Inherited
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageParam {
    /**
     * 消息属性的名称
     */
    String value();
}