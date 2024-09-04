package com.reopenai.component.pulsar.resolve;

import com.reopenai.component.pulsar.producer.invoker.ProducerMessage;
import org.springframework.core.MethodParameter;

/**
 * 生产者参数解析器
 *
 * @author Allen Huang
 */
public interface ProducerArgumentResolve {
    /**
     * 此解析器是否支持某一种参数
     *
     * @param parameterInfo 要解析的方法参数
     * @return 如果这个解析器支持这个方法参数则返回true，否则返回false
     */
    boolean supportsParameter(MethodParameter parameterInfo);

    /**
     * 根据参数内容将参数值添加到消息中
     *
     * @param parameterInfo 参数类型信息
     * @param value         参数值
     * @param message       生产者消息内容
     */
    void resolveArgument(MethodParameter parameterInfo, Object value, ProducerMessage message);

}
