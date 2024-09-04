package com.reopenai.component.pulsar.support;

/**
 * 框架内置的消息属性(Message properties)key约定.
 * 生产者写入, 消费者读取, 用于跨生产者/消费者的元信息传递.
 *
 * @author Allen Huang
 */
public final class MessageHeaders {

    private MessageHeaders() {
    }

    /**
     * 序列化协议. 生产者发送时写入, 消费者据此选择 {@code MessageConverter} 反序列化消息体.
     */
    public static final String PROTOCOL = "__pulsar.protocol__";

    /**
     * 消息唯一键(hostname + uuid). 生产者自动生成, 消费者据此去重/追踪.
     */
    public static final String UNIQUE_KEY = "__pulsar.uniqueKey__";

}
