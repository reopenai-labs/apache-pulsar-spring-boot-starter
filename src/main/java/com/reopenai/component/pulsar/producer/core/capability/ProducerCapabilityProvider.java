package com.reopenai.component.pulsar.producer.core.capability;

import com.reopenai.component.pulsar.producer.core.ProducerContext;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMethodInvoker;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 能力实现提供者. 为某能力接口的方法创建 {@link ProducerMethodInvoker}.
 * <p>
 * 实现此接口并注册为Spring Bean, 框架在生产者接口继承了 {@link #capabilityType()} 对应的能力接口时,
 * 自动调用 {@link #createInvokers} 为能力方法挂载实现.
 *
 * @param <A> 能力接口类型
 * @author Allen Huang
 */
public interface ProducerCapabilityProvider<A extends ProducerCapability> {

    /**
     * 此 provider 支持的能力接口
     *
     * @return 能力接口类型
     */
    Class<A> capabilityType();

    /**
     * 为该能力接口的方法创建 Invoker
     *
     * @param context 生产者上下文
     * @return 方法到 Invoker 的映射
     */
    Map<Method, ProducerMethodInvoker> createInvokers(ProducerContext context);

}
