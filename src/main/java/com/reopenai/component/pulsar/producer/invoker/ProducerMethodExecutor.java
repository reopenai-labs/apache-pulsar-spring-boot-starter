package com.reopenai.component.pulsar.producer.invoker;

import com.reopenai.component.pulsar.producer.ProducerContext;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.TypedMessageBuilder;
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
     * @param builder 消息构建对象
     * @return MessageId
     * @throws PulsarClientException 消息发送出错的时候抛出的异常
     */
    Object send(TypedMessageBuilder<byte[]> builder) throws PulsarClientException;

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
        public Object send(TypedMessageBuilder<byte[]> builder) {
            return builder.sendAsync().whenComplete((result, ex) -> {
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
        public Object send(TypedMessageBuilder<byte[]> builder) throws PulsarClientException {
            return builder.send();
        }

    }

}
