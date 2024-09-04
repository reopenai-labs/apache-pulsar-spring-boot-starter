package com.reopenai.component.pulsar.producer.extension.dynamic;

import com.reopenai.component.pulsar.producer.ProducerConfiguration;
import com.reopenai.component.pulsar.producer.ProducerContext;
import com.reopenai.component.pulsar.producer.annotation.ProducerMethod;
import com.reopenai.component.pulsar.producer.invoker.*;
import com.reopenai.component.pulsar.producer.register.ProducerRegister;
import com.reopenai.component.pulsar.serialization.MessageConverter;
import com.reopenai.component.pulsar.serialization.MessageConverterRegister;
import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Allen Huang
 */
@RequiredArgsConstructor
public class DynamicProducerAbilityInvokerFactory implements ProducerMethodInvokerFactory, ProducerMessageSupplierFactory {

    static String PREFIX = "extension.dynamic_producer";

    private final ProducerRegister producerRegister;

    private final MessageConverterRegister converterRegister;

    @Override
    public Map<Method, ProducerMethodInvoker> create(ProducerContext context) {
        Method[] methods = DynamicProducerAbility.class.getMethods();
        Map<Method, ProducerMethodInvoker> invokers = new HashMap<>(methods.length);
        for (Method method : methods) {
            if (method.getName().equals("addTopicByName")) {
                invokers.put(method, new AddTopicByNameInvoker(context, this.producerRegister));
            } else if (method.getName().equals("addTopicByFullName")) {
                invokers.put(method, new AddTopicFromFullNameInvoker(context, this.producerRegister));
            }
        }
        return invokers;
    }

    @Override
    public boolean canProduce(ProducerContext context, Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(ProducerSelect.class) && parameter.getType() == String.class) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ProducerMessageSupplier create(ProducerContext context, Method method) {
        ProducerMethod producerMethod = method.getAnnotation(ProducerMethod.class);
        String protocol = producerMethod.protocol();
        MessageConverter converter = converterRegister.get(protocol);
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(ProducerSelect.class) && parameter.getType() == String.class) {
                Logger logger = LoggerFactory.getLogger(context.getType());
                return new DynamicProducerMessageSupplier(i, logger, method, converter);
            }
        }
        return null;
    }


    @RequiredArgsConstructor
    public static class DynamicProducerMessageSupplier implements ProducerMessageSupplier {

        private final int index;

        private final Logger logger;

        private final Method method;

        private final MessageConverter converter;

        @Override
        public ProducerMessage get(ProducerContext context, Object[] arguments) {
            String alias = (String) arguments[index];
            String key = String.join(".", PREFIX, alias);
            Producer<byte[]> producer = context.getAttributes(key);
            if (producer == null) {
                String methodName = method.getName();
                String typeName = context.getType().getSimpleName();
                logger.warn("[DynamicProducer]@ProducerSelect找不到对应的Producer实例,将使用默认的Producer发送此消息.{}.{}(@ProducerSelect {})", typeName, methodName, alias);
                producer = context.getProducer();
            }
            TypedMessageBuilder<byte[]> builder = producer.newMessage();
            return new ProducerMessage(this.converter, builder);
        }

    }

    @RequiredArgsConstructor
    public static class AddTopicByNameInvoker implements ProducerMethodInvoker {

        private final ProducerContext context;

        private final ProducerRegister producerManager;

        @Override
        public Object invoke(Object[] arguments) {
            String alias = (String) arguments[0];
            if (!StringUtils.hasText(alias)) {
                throw new IllegalArgumentException("dynamic producer invoke error. alias cannot be null or empty.see: " + context.getType().getName());
            }
            String fullTopic = (String) arguments[1];
            ProducerConfiguration.Builder builder = context.getConfiguration().cloneBuilder();
            String newTopic = context.getTopic().replaceAll("(?<=/)[^/]*$", fullTopic);
            try {
                builder.topic(newTopic);
                Producer<byte[]> instance = producerManager.getInstance(builder.build());
                context.addAttributes(String.join(".", PREFIX, alias), instance);
                return null;
            } catch (PulsarClientException e) {
                throw new BeanCreationException("Failed to create producer.topic={}", newTopic, e);
            }
        }
    }

    @RequiredArgsConstructor
    public static class AddTopicFromFullNameInvoker implements ProducerMethodInvoker {

        private final ProducerContext context;

        private final ProducerRegister producerManager;

        @Override
        public Object invoke(Object[] arguments) {
            String alias = (String) arguments[0];
            if (!StringUtils.hasText(alias)) {
                throw new IllegalArgumentException("dynamic producer invoke error. alias cannot be null or empty.see: " + context.getType().getName());
            }
            String topic = (String) arguments[1];
            ProducerConfiguration.Builder builder = context.getConfiguration().cloneBuilder();
            builder.topic(topic);
            try {
                Producer<byte[]> instance = producerManager.getInstance(builder.build());
                context.addAttributes(String.join(".", PREFIX, alias), instance);
                return null;
            } catch (PulsarClientException e) {
                throw new BeanCreationException("Failed to create producer.topic={}", topic, e);
            }
        }

    }

}
