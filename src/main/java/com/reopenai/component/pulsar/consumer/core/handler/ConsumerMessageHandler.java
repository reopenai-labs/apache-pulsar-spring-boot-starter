package com.reopenai.component.pulsar.consumer.core.handler;

import com.reopenai.component.pulsar.consumer.core.ConsumerContext;
import com.reopenai.component.pulsar.consumer.core.invoker.ConsumerMessageRecord;
import org.springframework.core.Ordered;

/**
 * 消费者消息处理器. 在消息匹配 handler 后、handler 方法调用前后提供扩展点,
 * 用于埋点/追踪/指标等横切逻辑, 与 producer 端 {@code ProducerMessageHandler} 对称.
 * <p>
 * 实现此接口并注册为 Spring Bean 即可全局生效, 多个处理器按 {@link Ordered} 顺序执行.
 * 处理器异常被隔离(仅记录日志), 不影响消息消费与 ack.
 *
 * @author Allen Huang
 */
public interface ConsumerMessageHandler extends Ordered {

    /**
     * 消费前: 属性匹配通过、参数解析成功后、handler 方法调用之前.
     *
     * @param record  消息视图
     * @param context 消费上下文
     */
    default void consumeBefore(ConsumerMessageRecord record, ConsumerContext context) {
    }

    /**
     * 消费后: handler 方法调用之后(无论成功失败).
     *
     * @param record  消息视图
     * @param context 消费上下文
     * @param error   handler 方法抛出的异常, 成功为 {@code null}
     */
    default void consumeAfter(ConsumerMessageRecord record, ConsumerContext context, Throwable error) {
    }

    @Override
    default int getOrder() {
        return 0;
    }

}
