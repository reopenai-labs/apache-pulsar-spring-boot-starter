package com.reopenai.component.pulsar.producer.extension.dynamic;

import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessage;
import com.reopenai.component.pulsar.producer.core.resolver.ProducerArgumentResolver;
import org.springframework.core.MethodParameter;

/**
 * {@link ProducerSelect} 参数的解析器.
 * <p>
 * 该参数的值(alias)由 {@link ProducerSelectMessageFactoryProvider} 按位置读取, 用于在发送时路由到指定 Topic,
 * 不写入消息体, 故本解析器为 no-op —— 仅占位使参数解析阶段通过, 避免启动时抛"无法解析参数".
 *
 * @author Allen Huang
 */
public class ProducerSelectArgumentResolver implements ProducerArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ProducerSelect.class)
                && parameter.getParameterType() == String.class;
    }

    @Override
    public void resolveArgument(MethodParameter parameter, Object value, ProducerMessage message) {
        // no-op: alias 由 ProducerSelectMessageFactory 按位置读取用于路由, 不写入消息
    }

}
