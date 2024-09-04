package com.reopenai.component.pulsar.consumer.core.resolver;

import com.reopenai.component.pulsar.annotation.MessageParam;
import com.reopenai.component.pulsar.consumer.core.invoker.ConsumerMessageRecord;
import org.springframework.core.MethodParameter;

/**
 * 单个属性的参数解析器: 从消息属性中按 {@link MessageParam#value()} 读取指定属性值.
 *
 * @author Allen Huang
 */
public class ConsumerMessageParamArgumentResolver implements ConsumerArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MessageParam.class)
                && parameter.getParameterType() == String.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ConsumerMessageRecord record) {
        MessageParam param = parameter.getParameterAnnotation(MessageParam.class);
        return record.getProperty(param.value());
    }

}
