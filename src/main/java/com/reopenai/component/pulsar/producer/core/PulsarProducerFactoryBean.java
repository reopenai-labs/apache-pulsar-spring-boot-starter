package com.reopenai.component.pulsar.producer.core;

import com.reopenai.component.pulsar.annotation.ProducerBatchPolicy;
import com.reopenai.component.pulsar.annotation.PulsarProducer;
import com.reopenai.component.pulsar.producer.core.capability.ProducerCapability;
import com.reopenai.component.pulsar.producer.core.capability.ProducerCapabilityProvider;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMethodInvoker;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMethodInvokerFactory;
import lombok.Setter;
import org.apache.pulsar.client.api.Producer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生产者接口的 {@link FactoryBean}. 为 {@code @PulsarProducer} 接口创建代理实例.
 * <p>
 * 方法路由来源(合并后由 {@link PulsarProducerProxy} 分发):
 * <ul>
 *   <li>{@code @ProducerMethod} 方法 —— 由 {@link ProducerMethodInvokerFactory} 提供</li>
 *   <li>能力接口方法 —— 由 {@link ProducerCapabilityProvider} 提供(生产者接口继承了 {@link ProducerCapability} 子接口时)</li>
 * </ul>
 * 同一方法被重复注册时抛出 {@link BeanCreationException}.
 *
 * @author Allen Huang
 */
public class PulsarProducerFactoryBean implements FactoryBean<Object>, EnvironmentAware {

    @Setter
    private Class<?> type;

    protected Environment environment;

    @Autowired
    private ProducerRegistry producerRegistry;

    @Autowired
    private List<ProducerMethodInvokerFactory> producerMethodInvokerFactories;

    @Autowired
    private List<ProducerCapabilityProvider<?>> capabilityProviders;

    @Override
    public Object getObject() throws Exception {
        ProducerConfiguration configuration = buildConfiguration();
        Producer<byte[]> producer = producerRegistry.getProducer(configuration);
        ProducerContext context = new ProducerContext(type, producer, configuration);

        Map<Method, ProducerMethodInvoker> invokers = new HashMap<>();
        // @ProducerMethod 方法
        for (ProducerMethodInvokerFactory factory : producerMethodInvokerFactories) {
            registerInvokers(invokers, factory.create(context));
        }
        // 能力方法: 扫描生产者接口继承的 ProducerCapability 子接口, 匹配 Provider 挂载实现
        registerInvokers(invokers, resolveCapabilityInvokers(context));

        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        Class<?>[] proxiedInterfaces = ClassUtils.getAllInterfacesForClass(type);
        PulsarProducerProxy proxy = new PulsarProducerProxy(context, invokers);
        return Proxy.newProxyInstance(classLoader, proxiedInterfaces, proxy);
    }

    private void registerInvokers(Map<Method, ProducerMethodInvoker> invokers, Map<Method, ProducerMethodInvoker> source) {
        for (Map.Entry<Method, ProducerMethodInvoker> entry : source.entrySet()) {
            Method method = entry.getKey();
            if (invokers.containsKey(method)) {
                throw new BeanCreationException("创建Producer实例失败,方法 " + method + " 被多个Invoker/能力重复注册");
            }
            invokers.put(method, entry.getValue());
        }
    }

    /**
     * 扫描生产者接口继承的 {@link ProducerCapability} 子接口, 匹配 {@link ProducerCapabilityProvider}
     * 为能力方法创建Invoker. 新增能力无需修改本方法.
     */
    private Map<Method, ProducerMethodInvoker> resolveCapabilityInvokers(ProducerContext context) {
        Map<Method, ProducerMethodInvoker> capabilityInvokers = new HashMap<>();
        for (Class<?> iface : type.getInterfaces()) {
            if (iface == ProducerCapability.class || !ProducerCapability.class.isAssignableFrom(iface)) {
                continue;
            }
            ProducerCapabilityProvider<?> provider = findCapabilityProvider(iface);
            if (provider == null) {
                throw new BeanCreationException("生产者接口 " + type.getName()
                        + " 继承了能力接口 " + iface.getName() + ", 但未找到对应的 ProducerCapabilityProvider 实现");
            }
            capabilityInvokers.putAll(provider.createInvokers(context));
        }
        return capabilityInvokers;
    }

    private ProducerCapabilityProvider<?> findCapabilityProvider(Class<?> capabilityType) {
        for (ProducerCapabilityProvider<?> provider : capabilityProviders) {
            if (provider.capabilityType() == capabilityType) {
                return provider;
            }
        }
        return null;
    }

    protected ProducerConfiguration buildConfiguration() {
        PulsarProducer config = type.getAnnotation(PulsarProducer.class);
        ProducerConfiguration.Builder builder = ProducerConfiguration.builder()
                .topic(buildTopicName(config))
                .accessMode(config.accessMode())
                .hashingScheme(config.hashingScheme())
                .compressionType(config.compressionType())
                .blockIfQueueFull(config.blockIfQueueFull())
                .sendTimeout(config.sendTimeout());

        String producerName = config.producerName();
        if (StringUtils.hasText(producerName)) {
            producerName = this.environment.resolvePlaceholders(producerName);
            builder.producerName(producerName);
        }

        ProducerBatchPolicy batchPolicy = config.batch();
        if (batchPolicy.enabled()) {
            builder.batchingEnabled(true)
                    .batchingMaxBytes(batchPolicy.batchingMaxBytes())
                    .batchingMaxMessages(batchPolicy.batchingMaxMessages())
                    .batchingMaxPublishDelay(batchPolicy.batchingMaxPublishDelay())
                    .roundRobinRouterBatchingPartitionSwitchFrequency(batchPolicy.roundRobinRouterBatchingPartitionSwitchFrequency());
        }
        return builder.build();
    }


    /**
     * 通过配置解析Topic
     *
     * @param config 配置注解
     * @return 拼装后的Topic
     */
    private String buildTopicName(PulsarProducer config) {
        String tenant = config.tenant();
        if (StringUtils.hasText(tenant)) {
            tenant = this.environment.resolvePlaceholders(tenant);
        }
        String namespace = config.namespace();
        if (StringUtils.hasText(namespace)) {
            namespace = this.environment.resolvePlaceholders(namespace);
        }
        String topicName = config.topicName();
        if (StringUtils.hasText(topicName)) {
            topicName = this.environment.resolvePlaceholders(topicName);
        }
        String scheme = config.persistentMode() ? "persistent" : "non-persistent";
        return scheme + "://" + tenant + "/" + namespace + "/" + topicName;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

}
