package com.reopenai.component.pulsar.consumer.core.resolver;

import com.reopenai.component.pulsar.consumer.core.invoker.ConsumerMessageRecord;
import org.springframework.core.MethodParameter;

/**
 * 消费者参数解析器.
 * <p>
 * 消费者方法参数解析的SPI接口. 实现此接口并注册为Spring Bean,
 * 即可扩展消费者方法的参数解析能力(如从消息中反序列化消息体、提取属性、消息ID、Topic等).
 *
 * @author Allen Huang
 */
public interface ConsumerArgumentResolver {

    /**
     * 此解析器是否支持某一种参数
     *
     * @param parameter 要解析的方法参数
     * @return 如果这个解析器支持这个方法参数则返回true，否则返回false
     */
    boolean supportsParameter(MethodParameter parameter);

    /**
     * 根据实际的消息内容以及方法的参数类型解析此类型对应的参数
     *
     * @param parameter 参数类型信息
     * @param message   消费者消息记录
     * @return 解析后的参数值
     */
    Object resolveArgument(MethodParameter parameter, ConsumerMessageRecord message);

}
