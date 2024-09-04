package com.reopenai.component.pulsar.producer.extension.dynamic;

import com.reopenai.component.pulsar.producer.register.ProducerRegister;
import com.reopenai.component.pulsar.serialization.MessageConverterRegister;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Allen Huang
 */
@Configuration
public class DynamicProducerAutoConfiguration {

    @Bean
    public DynamicProducerAbilityInvokerFactory dynamicProducerAbilityInvokerFactory(ProducerRegister producerRegister,
                                                                                     MessageConverterRegister converterRegister) {
        return new DynamicProducerAbilityInvokerFactory(producerRegister, converterRegister);
    }

}
