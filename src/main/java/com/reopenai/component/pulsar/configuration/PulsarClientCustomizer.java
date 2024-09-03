package com.reopenai.component.pulsar.configuration;

import org.apache.pulsar.client.api.PulsarClient;

/**
 * @author Allen Huang
 */
public interface PulsarClientCustomizer {

    PulsarClient customize(PulsarClient client);

}
