package com.reopenai.component.pulsar.producer.core.invoker;

import com.reopenai.component.pulsar.producer.core.ProducerContext;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;

/**
 * Invoker的执行器，将发送任务委托给Executor对象处理
 *
 * @author Allen Huang
 */
public interface ProducerMethodExecutor {

    /**
     * 将消息发送到PulsarBroker
     *
     * @param message 消息对象
     * @return MessageId(同步) 或 CompletableFuture(异步)
     * @throws PulsarClientException 消息发送出错的时候抛出的异常
     */
    Object send(ProducerMessage message) throws PulsarClientException;

    /**
     * 异步消息发送的执行器
     */
    class AsyncProducerMethodExecutor implements ProducerMethodExecutor {

        private final String topic;

        private final Logger logger;

        public AsyncProducerMethodExecutor(ProducerContext context) {
            this.topic = context.getTopic();
            this.logger = context.getLogger();
        }

        @Override
        public Object send(ProducerMessage message) {
            return message.sendAsync().whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("[PulsarProducer][{}]异步消息发送失败: {}", topic, ex.getMessage(), ex);
                }
            });
        }

    }

    /**
     * 同步消息发送执行器
     *
     * @author Allen Huang
     */
    class SyncProducerMethodExecutor implements ProducerMethodExecutor {

        @Override
        public Object send(ProducerMessage message) throws PulsarClientException {
            return message.send();
        }

    }

}
