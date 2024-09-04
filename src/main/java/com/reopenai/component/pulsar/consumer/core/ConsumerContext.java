package com.reopenai.component.pulsar.consumer.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单条消息的消费上下文. 封装 {@link Consumer} 与当前 {@link Message}, 并提供 ack/nack/重试的幂等结算
 * (每条消息最多结算一次, 重复调用返回 {@code false}). 当 {@code @ConsumerHandler} 方法声明
 * {@link ConsumerContext} 参数时, 由用户自行决定何时结算(手动 ack 模式).
 * <p>
 * 结算状态用 {@link AtomicBoolean}: 即使用户在多线程下并发调用结算方法, 也只会生效一次.
 *
 * @author Allen Huang
 */
@Getter
@RequiredArgsConstructor
public class ConsumerContext {

    private final Consumer<byte[]> consumer;

    private final Message<byte[]> message;

    /**
     * 该消息是否已被结算(ack/nack/reconsume), 用 CAS 保证只结算一次.
     */
    private final AtomicBoolean acknowledged = new AtomicBoolean();

    /**
     * 确认消息(成功消费). 仅首次调用生效, 重复调用返回 {@code false}.
     *
     * @return 首次结算返回 {@code true}, 已结算过返回 {@code false}
     */
    public boolean doAcknowledge() throws PulsarClientException {
        if (acknowledged.compareAndSet(false, true)) {
            consumer.acknowledge(this.message);
            return true;
        }
        return false;
    }

    /**
     * 否定确认, 触发消息重试投递. 仅首次调用生效.
     *
     * @return 首次结算返回 {@code true}, 已结算过返回 {@code false}
     */
    public boolean doNegativeAcknowledge() throws PulsarClientException {
        if (acknowledged.compareAndSet(false, true)) {
            consumer.negativeAcknowledge(this.message);
            return true;
        }
        return false;
    }

    /**
     * 延迟后重新消费该消息. 仅首次调用生效.
     *
     * @param delay    延迟时间
     * @param timeUnit 时间单位
     * @return 首次结算返回 {@code true}, 已结算过返回 {@code false}
     */
    public boolean doReconsumeLater(long delay, TimeUnit timeUnit) throws PulsarClientException {
        if (acknowledged.compareAndSet(false, true)) {
            consumer.reconsumeLater(this.message, delay, timeUnit);
            return true;
        }
        return false;
    }

}
