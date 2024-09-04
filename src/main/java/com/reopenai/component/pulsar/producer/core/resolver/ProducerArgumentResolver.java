package com.reopenai.component.pulsar.producer.core.resolver;

import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessage;
import org.springframework.core.MethodParameter;

/**
 * 生产者参数解析器
 *
 * @author Allen Huang
 */
public interface ProducerArgumentResolver {
    /**
     * 此解析器是否支持某一种参数
     *
     * @param parameter 要解析的方法参数
     * @return 如果这个解析器支持这个方法参数则返回true，否则返回false
     */
    boolean supportsParameter(MethodParameter parameter);

    /**
     * 根据参数内容将参数值添加到消息中
     *
     * @param parameter 参数类型信息
     * @param value         参数值
     * @param message       生产者消息内容
     */
    void resolveArgument(MethodParameter parameter, Object value, ProducerMessage message);

}
