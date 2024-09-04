package com.reopenai.component.pulsar.producer.invoker;

import org.apache.pulsar.client.api.PulsarClientException;

/**
 * @author Allen Huang
 */
public interface ProducerMethodInvoker {

    /**
     * 将消息上下文中的消息内容发送到消息队列中。
     * 此方法会解析方法参数，将方法参数转换成具体的消息内容或消息属性。
     * 共实现了两种发送模式，即同步发送和异步发送。
     * 在发送消息时会根据定义的方法返回值类型自动选择发送模式。
     * 如果定义的方法返回类型为{@link org.apache.pulsar.client.api.MessageId}或{@link Void}，则消息会同步发送。
     * 如果定义的方法返回类型为{@link java.util.concurrent.CompletableFuture}，则消息会采用异步方式发送。
     *
     * @return 发送消息的返回值. 如果是同步发送, 则将返回MessageId,如果是异步发送, 则将返回CompletableFuture.
     */
    Object invoke(Object[] arguments) throws PulsarClientException;

}
