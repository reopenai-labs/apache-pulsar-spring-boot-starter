package com.reopenai.component.pulsar.producer;

import org.apache.pulsar.client.api.Producer;
import org.springframework.core.Ordered;

/**
 * producer扩展接口，实现了Ordered接口，这就意味着Ordered越小的越先被执行.
 * 此接口的一种使用场景是对Producer实例进行装饰，扩展Producer的能力
 *
 * @author Allen Huang
 */
public interface ProducerCustomizer extends Ordered {

    /**
     * 自定义Producer的扩展
     *
     * @param producer Producer实例
     * @return 扩展后的Producer实例
     */
    Producer<byte[]> customize(Producer<byte[]> producer);

    @Override
    default int getOrder() {
        return 0;
    }

}
