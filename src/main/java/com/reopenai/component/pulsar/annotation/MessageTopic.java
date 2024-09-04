package com.reopenai.component.pulsar.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记获取Message的Topic
 * 被标记的字段必须是{@link java.lang.String}类型
 *
 * @author Allen Huang
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageTopic {
}
