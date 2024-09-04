package com.reopenai.component.pulsar.producer.extension.dynamic;

import com.reopenai.component.pulsar.annotation.ProducerMethod;
import com.reopenai.component.pulsar.producer.core.ProducerContext;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessage;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessageFactory;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessageFactoryProvider;
import com.reopenai.component.pulsar.serialization.MessageConverter;
import com.reopenai.component.pulsar.serialization.MessageConverterRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * {@link ProducerSelect} 发送路由实现. 当 {@code @ProducerMethod} 方法含 {@code @ProducerSelect} 参数时,
 * 据该参数值(alias)从 {@link ProducerContext} 选择对应Topic的Producer发送, 实现一个接口多Topic发送.
 * <p>
 * 与 {@link DynamicTopicCapabilityProvider} 解耦: 后者负责"添加Topic"(能力方法), 本类负责"选择Topic发送"(发送路由).
 *
 * @author Allen Huang
 */
public class ProducerSelectMessageFactoryProvider implements ProducerMessageFactoryProvider {

    private final MessageConverterRegistry converterRegistry;

    public ProducerSelectMessageFactoryProvider(MessageConverterRegistry converterRegistry) {
        this.converterRegistry = converterRegistry;
    }

    @Override
    public boolean supports(ProducerContext context, Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(ProducerSelect.class) && parameter.getType() == String.class) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ProducerMessageFactory create(ProducerContext context, Method method) {
        ProducerMethod producerMethod = method.getAnnotation(ProducerMethod.class);
        MessageConverter converter = converterRegistry.get(producerMethod.protocol());
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(ProducerSelect.class) && parameter.getType() == String.class) {
                Logger logger = LoggerFactory.getLogger(context.getProducerType());
                return new ProducerSelectMessageFactory(i, logger, method, converter);
            }
        }
        return null;
    }

    /**
     * 按 alias 从上下文选择Producer; 找不到时回退到默认Producer并告警.
     */
    @RequiredArgsConstructor
    public static class ProducerSelectMessageFactory implements ProducerMessageFactory {

        private final int index;

        private final Logger logger;

        private final Method method;

        private final MessageConverter converter;

        @Override
        public ProducerMessage get(ProducerContext context, Object[] arguments) {
            String alias = (String) arguments[index];
            Producer<byte[]> producer = context.getAttribute(DynamicTopicCapabilityProvider.attributeKey(alias));
            if (producer == null) {
                logger.warn("[DynamicProducer]@ProducerSelect找不到对应的Producer实例,将使用默认的Producer发送此消息.{}.{}(@ProducerSelect {})",
                        context.getProducerType().getSimpleName(), method.getName(), alias);
                producer = context.getProducer();
            }
            TypedMessageBuilder<byte[]> builder = producer.newMessage();
            return new ProducerMessage(converter, builder);
        }
    }

}
