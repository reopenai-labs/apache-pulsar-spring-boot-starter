package com.reopenai.component.pulsar.producer.core.capability;

/**
 * 生产者能力标记接口.
 * <p>
 * 生产者接口继承 {@code ProducerCapability} 的子接口即声明拥有该能力, 框架自动为能力方法提供实现
 * (通过 {@link ProducerCapabilityProvider}). 这套机制类似 Spring Data Repository 继承
 * {@code CrudRepository} 等接口获得能力 —— 新增一个能力只需定义能力接口(extends ProducerCapability)
 * + 实现 {@link ProducerCapabilityProvider} 并注册为Bean, 无需修改核心装配.
 *
 * @author Allen Huang
 */
public interface ProducerCapability {
}
