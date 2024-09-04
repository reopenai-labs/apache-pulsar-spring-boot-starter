package com.reopenai.component.pulsar.utils;

import com.reopenai.component.pulsar.annotation.MessageProperty;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * 属性工具类
 *
 * @author Allen Huang
 */
public final class MessageUtil {

    /**
     * 解析MessageProperty定义的属性列表.此方法会返回一个不可变集合，请勿尝试对此返回的内容进行写操作
     *
     * @param properties MessageProperty定义
     * @return 不为null的不可变集合
     */
    public static Map<String, String> parseProperties(MessageProperty[] properties) {
        if (properties.length > 0) {
            return Stream.of(properties)
                    .collect(toUnmodifiableMap(MessageProperty::key, MessageProperty::value));
        }
        return Collections.emptyMap();
    }

}
