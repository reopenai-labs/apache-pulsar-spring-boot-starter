package com.reopenai.component.pulsar.producer;

import com.reopenai.component.pulsar.producer.annotation.ProducerBatchPolicy;
import com.reopenai.component.pulsar.producer.annotation.PulsarProducer;
import com.reopenai.component.pulsar.producer.invoker.ProducerMethodInvoker;
import com.reopenai.component.pulsar.producer.invoker.ProducerMethodInvokerFactory;
import com.reopenai.component.pulsar.producer.register.ProducerRegister;
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
 * @author Allen Huang
 */
public class PulsarProducerFactoryBean implements FactoryBean<Object>, EnvironmentAware {

    @Setter
    private Class<?> type;

    protected Environment environment;

    @Autowired
    private ProducerRegister producerManager;

    @Autowired
    private List<ProducerMethodInvokerFactory> producerMethodInvokerFactories;

    @Override
    public Object getObject() throws Exception {
        ProducerConfiguration configuration = parseConfiguration();
        Producer<byte[]> producer = producerManager.getInstance(configuration);
        ProducerContext context = new ProducerContext(type, producer, configuration);

        Map<Method, ProducerMethodInvoker> invokers = new HashMap<>();
        for (ProducerMethodInvokerFactory producerMethodInvokerFactory : producerMethodInvokerFactories) {
            Map<Method, ProducerMethodInvoker> methodInvokers = producerMethodInvokerFactory.create(context);
            for (Map.Entry<Method, ProducerMethodInvoker> entry : methodInvokers.entrySet()) {
                Method method = entry.getKey();
                if (invokers.containsKey(method)) {
                    throw new BeanCreationException("创建Producer实例失败," + method + "尝试创建了多个Invoker实例");
                }
                invokers.put(method, entry.getValue());
            }
        }
        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        Class<?>[] proxiedInterfaces = ClassUtils.getAllInterfaces(type);
        PulsarProducerProxy proxy = new PulsarProducerProxy(context, invokers);
        return Proxy.newProxyInstance(classLoader, proxiedInterfaces, proxy);
    }

    protected ProducerConfiguration parseConfiguration() {
        PulsarProducer config = type.getAnnotation(PulsarProducer.class);
        ProducerConfiguration.Builder builder = ProducerConfiguration.builder()
                .topic(parseTopic(config))
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
        if (batchPolicy.enables()) {
            builder.batchingEnabled(true)
                    .batchingMaxBytes(batchPolicy.batchingMaxBytes())
                    .batchingMaxMessages(batchPolicy.batchingMaxMessages())
                    .batchingMaxPublishDelay(batchPolicy.batchingMaxPublishDelay())
                    .roundRobinRouterBatchingPartitionSwitchFrequency(batchPolicy.roundRobinRouterBatchingPartitionSwitchFrequency());
        }
        builder.builder(builder);
        return builder.build();
    }


    /**
     * 通过配置解析Topic
     *
     * @param config 配置注解
     * @return 拼装后的Topic
     */
    private String parseTopic(PulsarProducer config) {
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
        return config.persistentMode().createTopic(tenant, namespace, topicName);
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
