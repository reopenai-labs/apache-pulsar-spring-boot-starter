package com.reopenai.component.pulsar.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * topic的持久化模式
 *
 * @author Allen Huang
 */
@Getter
@RequiredArgsConstructor
public enum PersistentMode {

    /**
     * 持久化模式的topic
     */
    PERSISTENT("persistent://%s/%s/%s"),

    /**
     * 非持久化模式的topic
     */
    NON_PERSISTENT("non-persistent://%s/%s/%s");

    private final String template;

    public String createTopic(String tenant, String namespace, String topicName) {
        if (!StringUtils.hasLength(tenant)) {
            tenant = "public";
        }
        if (!StringUtils.hasLength(namespace)) {
            namespace = "default";
        }
        return String.format(template, tenant, namespace, topicName);
    }

}