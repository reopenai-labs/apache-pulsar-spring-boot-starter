package com.reopenai.component.pulsar.resolve;

import org.apache.pulsar.client.api.Message;
import org.springframework.core.MethodParameter;

/**
 * 消费者参数解析器
 *
 * @author Allen Huang
 */
public interface ConsumerArgumentResolve {

    /**
     * 此解析器是否支持某一种参数
     *
     * @param parameterInfo 要解析的方法参数
     * @return 如果这个解析器支持这个方法参数则返回true，否则返回false
     */
    boolean supportsParameter(MethodParameter parameterInfo);

    /**
     * 根据实际的消息内容以及方法的参数类型解析此类型对应的参数
     *
     * @param parameterInfo 参数类型
     * @param message       消息内容
     * @return 实际的参数结果
     */
    Object resolveArgument(MethodParameter parameterInfo, Message<byte[]> message);

}
