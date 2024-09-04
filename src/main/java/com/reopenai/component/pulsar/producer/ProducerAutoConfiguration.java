package com.reopenai.component.pulsar.producer;

import com.reopenai.component.pulsar.producer.invoker.DefaultProducerMethodInvokerFactory;
import com.reopenai.component.pulsar.producer.invoker.ProducerMessageSupplierFactory;
import com.reopenai.component.pulsar.producer.invoker.ProducerMethodInvokerFactory;
import com.reopenai.component.pulsar.producer.register.DefaultProducerRegister;
import com.reopenai.component.pulsar.producer.register.ProducerRegister;
import com.reopenai.component.pulsar.resolve.ProducerArgumentResolve;
import com.reopenai.component.pulsar.serialization.MessageConverterRegister;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Allen Huang
 */
@Configuration
public class ProducerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ProducerMethodInvokerFactory.class)
    public ProducerMethodInvokerFactory producerMethodInvokerFactory(MessageConverterRegister register,
                                                                     List<ProducerMessageSupplierFactory> messageSuppliers,
                                                                     List<ProducerArgumentResolve> argumentResolves) {
        return new DefaultProducerMethodInvokerFactory(register, messageSuppliers, argumentResolves);
    }

    @Bean
    @ConditionalOnMissingBean(ProducerRegister.class)
    public ProducerRegister producerRegister(PulsarClient pulsarClient,
                                             ObjectProvider<ProducerCustomizer> producerCustomizers,
                                             ObjectProvider<ProducerBuilderCustomizer> builderCustomizers) {
        return new DefaultProducerRegister(pulsarClient, producerCustomizers, builderCustomizers);
    }

}
