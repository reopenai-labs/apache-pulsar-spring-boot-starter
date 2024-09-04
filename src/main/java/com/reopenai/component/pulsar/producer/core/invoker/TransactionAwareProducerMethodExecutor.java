package com.reopenai.component.pulsar.producer.core.invoker;

import com.reopenai.component.pulsar.producer.core.ProducerContext;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;

/**
 * 事务感知的 {@link ProducerMethodExecutor} 装饰器.
 * <p>
 * 当方法配置为 {@code @ProducerMethod(transaction = TransactionPhase.AFTER_COMMIT)} 且当前存在激活的Spring事务时,
 * 将发送动作延迟到事务成功提交后执行(afterCommit); 事务回滚则不发送, 保证DB与消息的最终一致性.
 * 无激活事务时退化为立即发送.
 * <p>
 * 设计为装饰器而非继承, 可组合在任意 Sync/Async executor 之上, 符合开闭原则.
 * <p>
 * 返回值语义: 异步模式立即返回 {@code CompletableFuture}(事务提交后完成, 回滚则异常完成);
 * 同步模式立即返回 {@code null}——此时消息尚未发送(须待事务提交), 因此同步 {@code AFTER_COMMIT} 方法的
 * 返回值无意义, 调用方不应依赖其拿到 {@code MessageId}.
 *
 * @author Allen Huang
 */
public class TransactionAwareProducerMethodExecutor implements ProducerMethodExecutor {

    private final ProducerMethodExecutor delegate;

    private final boolean async;

    private final Logger logger;

    private final String topic;

    public TransactionAwareProducerMethodExecutor(ProducerMethodExecutor delegate, boolean async, ProducerContext context) {
        this.delegate = delegate;
        this.async = async;
        this.logger = context.getLogger();
        this.topic = context.getTopic();
    }

    @Override
    public Object send(ProducerMessage message) throws PulsarClientException {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            // 无激活事务, 立即发送
            return delegate.send(message);
        }
        if (async) {
            return registerAsyncAfterCommit(message);
        }
        registerSyncAfterCommit(message);
        return null;
    }

    /**
     * 同步模式: 方法立即返回(null), 在事务afterCommit时阻塞发送. 失败仅记录日志(事务已提交,无法回滚).
     */
    private void registerSyncAfterCommit(ProducerMessage message) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    delegate.send(message);
                } catch (PulsarClientException e) {
                    logger.error("[Pulsar][Producer][{}]事务提交后发送消息失败, DB已提交但消息可能丢失.", topic, e);
                }
            }
        });
    }

    /**
     * 异步模式: 方法立即返回CompletableFuture, 在事务afterCommit时完成; 回滚则异常完成.
     */
    private CompletableFuture<MessageId> registerAsyncAfterCommit(ProducerMessage message) {
        CompletableFuture<MessageId> future = new CompletableFuture<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    @SuppressWarnings("unchecked")
                    CompletableFuture<MessageId> delegateFuture = (CompletableFuture<MessageId>) delegate.send(message);
                    delegateFuture.whenComplete((id, err) -> {
                        if (err != null) {
                            future.completeExceptionally(err);
                        } else {
                            future.complete(id);
                        }
                    });
                } catch (PulsarClientException e) {
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (STATUS_ROLLED_BACK == status) {
                    logger.warn("[Pulsar][Producer][{}]事务回滚, 消息不会发送.", topic);
                    future.completeExceptionally(new IllegalStateException("事务已回滚, 消息不会发送. topic=" + topic));
                }
            }
        });
        return future;
    }

}
