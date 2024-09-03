package com.reopenai.component.pulsar.configuration;

import org.apache.pulsar.client.admin.PulsarAdminBuilder;

/**
 * @author Allen Huang
 */
public interface PulsarAdminBuilderCustomizer {

    void customize(PulsarAdminBuilder builder);

}
