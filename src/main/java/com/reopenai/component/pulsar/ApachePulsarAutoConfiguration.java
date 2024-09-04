package com.reopenai.component.pulsar;

import com.reopenai.component.pulsar.consumer.core.PulsarConsumerBeanPostProcessor;
import com.reopenai.component.pulsar.consumer.core.ConsumerConfiguration;
import com.reopenai.component.pulsar.consumer.core.ConsumerRegistry;
import com.reopenai.component.pulsar.consumer.core.resolver.ConsumerEventTimeArgumentResolver;
import com.reopenai.component.pulsar.consumer.core.resolver.ConsumerMessageIdArgumentResolver;
import com.reopenai.component.pulsar.consumer.core.resolver.ConsumerMessageKeyArgumentResolver;
import com.reopenai.component.pulsar.consumer.core.resolver.ConsumerMessageParamArgumentResolver;
import com.reopenai.component.pulsar.consumer.core.resolver.ConsumerMessageParamsArgumentResolver;
import com.reopenai.component.pulsar.consumer.core.resolver.ConsumerMessageTopicArgumentResolver;
import com.reopenai.component.pulsar.consumer.core.resolver.ConsumerMessageValueArgumentResolver;
import com.reopenai.component.pulsar.producer.core.ProducerRegistry;
import com.reopenai.component.pulsar.producer.core.PulsarProducerRegistrar;
import com.reopenai.component.pulsar.producer.core.handler.ProducerMessageHandler;
import com.reopenai.component.pulsar.producer.core.invoker.DefaultProducerMethodInvokerFactory;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMessageFactoryProvider;
import com.reopenai.component.pulsar.producer.core.invoker.ProducerMethodInvokerFactory;
import com.reopenai.component.pulsar.producer.core.resolver.EventTimeArgumentResolver;
import com.reopenai.component.pulsar.producer.core.resolver.LocalDateTimeEventTimeArgumentResolver;
import com.reopenai.component.pulsar.producer.core.resolver.MessageKeyArgumentResolver;
import com.reopenai.component.pulsar.producer.core.resolver.MessageParamArgumentResolver;
import com.reopenai.component.pulsar.producer.core.resolver.MessageParamsArgumentResolver;
import com.reopenai.component.pulsar.producer.core.resolver.MessageValueArgumentResolver;
import com.reopenai.component.pulsar.producer.core.resolver.ProducerArgumentResolver;
import com.reopenai.component.pulsar.producer.extension.dynamic.DynamicTopicCapabilityProvider;
import com.reopenai.component.pulsar.producer.extension.dynamic.ProducerSelectArgumentResolver;
import com.reopenai.component.pulsar.producer.extension.dynamic.ProducerSelectMessageFactoryProvider;
import com.reopenai.component.pulsar.serialization.JacksonMessageConverter;
import com.reopenai.component.pulsar.serialization.JdkMessageConverter;
import com.reopenai.component.pulsar.serialization.MessageConverter;
import com.reopenai.component.pulsar.serialization.MessageConverterRegistry;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Pulsar 自动配置入口, 集中装配框架所有基础组件.
 * <p>
 * <b>Producer</b>: 通过 {@link Import} 引入 {@link PulsarProducerRegistrar}, 默认自动扫描 main class 所在包下
 * 所有 {@code @PulsarProducer} 接口并注册为代理; 仅导入外部 Producer 时才用 {@code @ImportProducers}.
 * <p>
 * <b>Consumer</b>: {@link PulsarConsumerBeanPostProcessor} 扫描 {@code @PulsarConsumer} Bean,
 * 为其中的 {@code @ConsumerHandler} 方法创建 Pulsar 消费者并启动消费.
 * <p>
 * PulsarClient/PulsarAdmin 等底层客户端由 spring-boot-starter-pulsar 负责初始化, 本类不再重复装配.
 *
 * @author Allen Huang
 */
@Configuration
@Import(PulsarProducerRegistrar.class)
public class ApachePulsarAutoConfiguration {

    //--------------------------------
    //          序列化
    //--------------------------------

    @Bean
    public MessageConverterRegistry messageConverterRegistry(List<MessageConverter> converters) {
        return new MessageConverterRegistry(converters);
    }

    @Bean
    @ConditionalOnMissingBean(JacksonMessageConverter.class)
    public JacksonMessageConverter jacksonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonMessageConverter(jsonMapper);
    }

    @Bean
    @ConditionalOnMissingBean(JdkMessageConverter.class)
    public JdkMessageConverter jdkMessageConverter() {
        return new JdkMessageConverter();
    }

    //--------------------------------
    //          Producer 参数解析器
    //--------------------------------

    @Bean
    public MessageValueArgumentResolver messageValueArgumentResolver() {
        return new MessageValueArgumentResolver();
    }

    @Bean
    public MessageParamArgumentResolver messageParamArgumentResolver() {
        return new MessageParamArgumentResolver();
    }

    @Bean
    public MessageParamsArgumentResolver messageParamsArgumentResolver() {
        return new MessageParamsArgumentResolver();
    }

    @Bean
    public MessageKeyArgumentResolver messageKeyArgumentResolver() {
        return new MessageKeyArgumentResolver();
    }

    @Bean
    public EventTimeArgumentResolver eventTimeArgumentResolver() {
        return new EventTimeArgumentResolver();
    }

    @Bean
    public LocalDateTimeEventTimeArgumentResolver localDateTimeEventTimeArgumentResolver() {
        return new LocalDateTimeEventTimeArgumentResolver();
    }

    //--------------------------------
    //          Producer 核心
    //--------------------------------

    @Bean
    @ConditionalOnMissingBean(ProducerMethodInvokerFactory.class)
    public ProducerMethodInvokerFactory producerMethodInvokerFactory(MessageConverterRegistry converterRegistry,
                                                                     List<ProducerMessageFactoryProvider> messageFactoryProviders,
                                                                     List<ProducerArgumentResolver> argumentResolvers,
                                                                     List<ProducerMessageHandler> handlers) {
        return new DefaultProducerMethodInvokerFactory(converterRegistry, messageFactoryProviders, argumentResolvers, handlers);
    }

    @Bean
    @ConditionalOnMissingBean(ProducerRegistry.class)
    public ProducerRegistry producerRegistry(PulsarClient pulsarClient,
                                             ObjectProvider<ProducerBuilderCustomizer<byte[]>> builderCustomizers) {
        return new ProducerRegistry(pulsarClient, builderCustomizers);
    }

    //--------------------------------
    //          Producer 扩展能力(动态Topic)
    //--------------------------------

    @Bean
    @ConditionalOnMissingBean(DynamicTopicCapabilityProvider.class)
    public DynamicTopicCapabilityProvider dynamicTopicCapabilityProvider(ProducerRegistry producerRegistry) {
        return new DynamicTopicCapabilityProvider(producerRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(ProducerSelectMessageFactoryProvider.class)
    public ProducerSelectMessageFactoryProvider producerSelectMessageFactoryProvider(MessageConverterRegistry converterRegistry) {
        return new ProducerSelectMessageFactoryProvider(converterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(ProducerSelectArgumentResolver.class)
    public ProducerSelectArgumentResolver producerSelectArgumentResolver() {
        return new ProducerSelectArgumentResolver();
    }

    //--------------------------------
    //          Consumer
    //--------------------------------

    /**
     * 消费者扫描入口. 用 static @Bean 确保作为 BeanPostProcessor 提前注册, 不依赖本配置类实例化.
     */
    @Bean
    public static PulsarConsumerBeanPostProcessor pulsarConsumerBeanPostProcessor() {
        return new PulsarConsumerBeanPostProcessor();
    }

    @Bean
    @ConditionalOnMissingBean(ConsumerConfiguration.class)
    public ConsumerConfiguration consumerConfiguration(Environment environment) {
        return new ConsumerConfiguration(environment);
    }

    // 销毁顺序由 @Bean 定义顺序保证: producerRegistry 先定义(先初始化/后销毁),
    // consumerRegistry 后定义(后初始化/先销毁) → Consumer 先停止接收, Producer 后停止发送, 避免半截消息.
    @Bean
    @ConditionalOnMissingBean(ConsumerRegistry.class)
    public ConsumerRegistry consumerRegistry() {
        return new ConsumerRegistry();
    }

    @Bean
    public ConsumerMessageValueArgumentResolver consumerMessageValueArgumentResolver() {
        return new ConsumerMessageValueArgumentResolver();
    }

    @Bean
    public ConsumerMessageParamArgumentResolver consumerMessageParamArgumentResolver() {
        return new ConsumerMessageParamArgumentResolver();
    }

    @Bean
    public ConsumerMessageParamsArgumentResolver consumerMessageParamsArgumentResolver() {
        return new ConsumerMessageParamsArgumentResolver();
    }

    @Bean
    public ConsumerMessageKeyArgumentResolver consumerMessageKeyArgumentResolver() {
        return new ConsumerMessageKeyArgumentResolver();
    }

    @Bean
    public ConsumerMessageIdArgumentResolver consumerMessageIdArgumentResolver() {
        return new ConsumerMessageIdArgumentResolver();
    }

    @Bean
    public ConsumerMessageTopicArgumentResolver consumerMessageTopicArgumentResolver() {
        return new ConsumerMessageTopicArgumentResolver();
    }

    @Bean
    public ConsumerEventTimeArgumentResolver consumerEventTimeArgumentResolver() {
        return new ConsumerEventTimeArgumentResolver();
    }

}
