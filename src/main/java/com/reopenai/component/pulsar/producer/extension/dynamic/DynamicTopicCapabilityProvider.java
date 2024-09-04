package com.reopenai.component.pulsar.producer.extension.dynamic;

import com.reopenai.component.pulsar.producer.core.ProducerConfiguration;
import com.reopenai.component.pulsar.producer.core.ProducerContext;
import com.reopenai.component.pulsar.producer.core.ProducerRegistry;
import com.reopenai.component.pulsar.producer.core.capability.ProducerCapabilityProvider;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMethodInvoker;
import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link DynamicTopicCapability} 的实现. 为 {@code addTopicByName}/{@code addTopicByFullName} 创建 Invoker,
 * 运行时派生 {@link ProducerConfiguration} 并通过 {@link ProducerRegistry} 创建Producer,
 * 以 alias 为 key 缓存到 {@link ProducerContext} 供 {@link ProducerSelect} 路由使用.
 *
 * @author Allen Huang
 */
public class DynamicTopicCapabilityProvider implements ProducerCapabilityProvider<DynamicTopicCapability> {

    /**
     * 动态Producer在 {@link ProducerContext} 属性中的 key 前缀
     */
    static final String PREFIX = "extension.dynamic_producer";

    private final ProducerRegistry producerRegistry;

    public DynamicTopicCapabilityProvider(ProducerRegistry producerRegistry) {
        this.producerRegistry = producerRegistry;
    }

    @Override
    public Class<DynamicTopicCapability> capabilityType() {
        return DynamicTopicCapability.class;
    }

    @Override
    public Map<Method, ProducerMethodInvoker> createInvokers(ProducerContext context) {
        Map<Method, ProducerMethodInvoker> invokers = new HashMap<>();
        for (Method method : DynamicTopicCapability.class.getMethods()) {
            switch (method.getName()) {
                case "addTopicByName" -> invokers.put(method, new AddTopicByNameInvoker(context, producerRegistry));
                case "addTopicByFullName" -> invokers.put(method, new AddTopicByFullNameInvoker(context, producerRegistry));
                default -> { /* Object 方法等, 忽略 */ }
            }
        }
        return invokers;
    }

    /**
     * 生成 alias 对应的 ProducerContext 属性 key (供 {@link ProducerSelectMessageFactoryProvider} 共用)
     */
    static String attributeKey(String alias) {
        return PREFIX + "." + alias;
    }

    /**
     * 将完整 topic({@code scheme://tenant/namespace/topicName}) 的最后一段替换为 {@code newTopicName}.
     * 用 {@code lastIndexOf} 定位最后一段, 避免 {@code replaceAll} 把新名称当作正则 replacement
     * (含 {@code $}/{@code \} 时出错).
     */
    private static String replaceLastSegment(String fullTopic, String newTopicName) {
        int lastSlash = fullTopic.lastIndexOf('/');
        return fullTopic.substring(0, lastSlash + 1) + newTopicName;
    }

    @RequiredArgsConstructor
    static class AddTopicByNameInvoker implements ProducerMethodInvoker {

        private final ProducerContext context;

        private final ProducerRegistry producerRegistry;

        @Override
        public Object invoke(Object[] arguments) {
            String alias = (String) arguments[0];
            String topicName = (String) arguments[1];
            assertAlias(alias, context);
            // 用 topicName 替换原 topic 的最后一段, 保留 scheme://tenant/namespace
            String newTopic = replaceLastSegment(context.getTopic(), topicName);
            registerProducer(alias, newTopic);
            return null;
        }

        private void registerProducer(String alias, String topic) {
            try {
                ProducerConfiguration config = context.getConfiguration().toBuilder().topic(topic).build();
                Producer<byte[]> producer = producerRegistry.getProducer(config);
                context.addAttribute(attributeKey(alias), producer);
            } catch (PulsarClientException e) {
                throw new BeanCreationException("创建Producer失败.topic=" + topic, e);
            }
        }
    }

    @RequiredArgsConstructor
    static class AddTopicByFullNameInvoker implements ProducerMethodInvoker {

        private final ProducerContext context;

        private final ProducerRegistry producerRegistry;

        @Override
        public Object invoke(Object[] arguments) {
            String alias = (String) arguments[0];
            String topic = (String) arguments[1];
            assertAlias(alias, context);
            try {
                ProducerConfiguration config = context.getConfiguration().toBuilder().topic(topic).build();
                Producer<byte[]> producer = producerRegistry.getProducer(config);
                context.addAttribute(attributeKey(alias), producer);
            } catch (PulsarClientException e) {
                throw new BeanCreationException("创建Producer失败.topic=" + topic, e);
            }
            return null;
        }
    }

    private static void assertAlias(String alias, ProducerContext context) {
        if (!StringUtils.hasText(alias)) {
            throw new IllegalArgumentException(
                    "dynamic producer alias cannot be null or empty. see: " + context.getProducerType().getName());
        }
    }

}
