package com.reopenai.component.pulsar.producer.extension.transaction;

import org.apache.pulsar.client.api.transaction.Transaction;

/**
 * 创建事物消息的能力
 *
 * @author Allen Huang
 */
public interface TransactionProducerAbility {

    Transaction createTransaction();

}
