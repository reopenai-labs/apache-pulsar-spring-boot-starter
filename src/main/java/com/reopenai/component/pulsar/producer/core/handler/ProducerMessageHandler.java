package com.reopenai.component.pulsar.producer.core.handler;

import com.reopenai.component.pulsar.producer.core.ProducerContext;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessage;
import org.springframework.core.Ordered;

/**
 * 生产者消息处理器. 在消息发送前后提供扩展点, 用于埋点/追踪/指标/消息改写等横切逻辑.
 * <p>
 * 实现此接口并注册为Spring Bean即可全局生效, 多个处理器按 {@link Ordered} 顺序执行.
 *
 * @author Allen Huang
 */
public interface ProducerMessageHandler extends Ordered {

    /**
     * 发送前: {@link ProducerMessage} 构造完成、参数解析后、实际发送之前. 可修改message(如追加追踪属性).
     *
     * @param message 待发送的消息
     * @param context 生产者上下文
     */
    default void sendBefore(ProducerMessage message, ProducerContext context) {
    }

    /**
     * 发送后: 发送动作返回后.
     * 同步模式 {@code result} 为 {@code MessageId}; 异步模式 {@code result} 为 {@code CompletableFuture}(可能尚未完成).
     * {@code error} 非 {@code null} 表示发送抛出异常.
     *
     * @param message 已发送的消息
     * @param result  发送结果(同步为MessageId, 异步为CompletableFuture)
     * @param error   发送异常, 无异常为null
     * @param context 生产者上下文
     */
    default void sendAfter(ProducerMessage message, Object result, Throwable error, ProducerContext context) {
    }

    @Override
    default int getOrder() {
        return 0;
    }

}
