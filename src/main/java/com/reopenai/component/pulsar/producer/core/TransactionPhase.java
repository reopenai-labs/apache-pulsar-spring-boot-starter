package com.reopenai.component.pulsar.producer.core;

/**
 * 生产者消息的事务感知发送策略.
 *
 * @author Allen Huang
 */
public enum TransactionPhase {

    /**
     * 立即发送, 不感知Spring事务(默认).
     */
    IMMEDIATE,

    /**
     * 仅在Spring事务(@Transactional)成功提交后才发送; 事务回滚则不发送, 保证DB与消息一致性.
     */
    AFTER_COMMIT
}
