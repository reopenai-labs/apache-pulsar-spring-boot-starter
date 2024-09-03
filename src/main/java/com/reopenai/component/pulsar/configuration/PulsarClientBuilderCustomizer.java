package com.reopenai.component.pulsar.configuration;

import org.apache.pulsar.client.api.ClientBuilder;

/**
 * @author Allen Huang
 */
public interface PulsarClientBuilderCustomizer {

    void customize(ClientBuilder builder);

}
