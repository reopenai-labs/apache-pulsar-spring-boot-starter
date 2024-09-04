package com.reopenai.component.pulsar.consumer.core.resolver;

import com.reopenai.component.pulsar.annotation.MessageValue;
import com.reopenai.component.pulsar.consumer.core.invoker.ConsumerMessageRecord;
import org.springframework.core.MethodParameter;

/**
 * 消息内容解析器: 使用绑定的 {@link com.reopenai.component.pulsar.serialization.MessageConverter}
 * 将消息荷载反序列化为方法参数类型.
 *
 * @author Allen Huang
 */
public class ConsumerMessageValueArgumentResolver implements ConsumerArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MessageValue.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ConsumerMessageRecord record) {
        return record.messageConverter().deserialize(record.getData(), parameter);
    }

}
