package com.reopenai.component.pulsar.annotation;

import com.reopenai.component.pulsar.serialization.MessageProtocol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为Pulsar消息处理器.
 *
 * @author Allen Huang
 * @see PulsarConsumer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface ConsumerHandler {
    /**
     * 指定序列化协议，默认为JSON序列化
     */
    String protocol() default MessageProtocol.JSON;

    /**
     * 如果指定了属性列表，那么只有在消息中存在指定的所有属性时，此处理器才会处理这条消息。例如:<br>
     * 接收到某条消息，其中消息中的属性为: key1=value1,key2=value2,key3=value3.
     * <ul>
     *     <li>如果处理器中指定的属性列表为: key1=value1,key2=value2，那么这个处理器将会处理这条消息</li>
     *     <li>如果处理器中指定的属性列表为: key1=value1,key2=value2,key3=value3，那么这个处理器将会处理这条消息</li>
     *     <li>如果处理器中指定的属性列表为: key1=value1,key4=value4，那么这个处理器将不会处理这条消息</li>
     * </ul>
     * 要想满足匹配条件，handler的属性列表必须是消息属性列表的子集。如果此参数未设置值，那么将处理所有的消息。
     */
    MessageProperty[] properties() default {};

    /**
     * 是否忽略反序列化错误
     *
     * @return 默认不忽略反序列化错误
     */
    boolean ignoreDeserializeFail() default false;

    /**
     * 当消费一条消息出现异常的时候，消息不会被确认，Pulsar将会重试消费。
     * 此参数的意义是允许handler抛出一些异常，并且抛出这些异常的时候handler会正常的ack，完全当作消息被正常消费了。
     */
    Class<? extends Throwable>[] noNegativeFor() default {};

}
