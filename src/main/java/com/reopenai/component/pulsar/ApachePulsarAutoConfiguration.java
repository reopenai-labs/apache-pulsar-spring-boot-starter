package com.reopenai.component.pulsar;

import com.reopenai.component.pulsar.configuration.PulsarAdminBuilderCustomizer;
import com.reopenai.component.pulsar.configuration.PulsarClientBuilderCustomizer;
import com.reopenai.component.pulsar.configuration.PulsarClientCustomizer;
import com.reopenai.component.pulsar.configuration.PulsarProperties;
import com.reopenai.component.pulsar.serialization.DefaultMessageConverterRegister;
import com.reopenai.component.pulsar.serialization.MessageConverter;
import com.reopenai.component.pulsar.serialization.MessageConverterRegister;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.auth.oauth2.AuthenticationOAuth2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Pulsar 客户端配置
 *
 * @author Allen Huang
 */
@Configuration
@EnableConfigurationProperties(PulsarProperties.class)
public class ApachePulsarAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(PulsarAdmin.class)
    @ConditionalOnProperty(value = "pulsar.admin.service-http-url")
    public PulsarAdmin pulsarAdmin(PulsarProperties properties, ObjectProvider<PulsarAdminBuilderCustomizer> builderCustomizers) throws PulsarClientException {
        PulsarProperties.Admin admin = properties.getAdmin();
        PulsarAdminBuilder builder = PulsarAdmin.builder()
                .serviceHttpUrl(admin.getServiceHttpUrl())
                .readTimeout((int) admin.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .requestTimeout((int) admin.getRequestTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .connectionTimeout((int) admin.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS);
        builderCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(PulsarClient.class)
    public PulsarClient pulsarClient(PulsarProperties properties,
                                     ObjectProvider<PulsarClientCustomizer> clientCustomizers,
                                     ObjectProvider<PulsarClientBuilderCustomizer> builderCustomizers) throws PulsarClientException {
        PulsarProperties.Client client = properties.getClient();
        ClientBuilder builder = PulsarClient.builder()
                .ioThreads(client.getIoThreads())
                .serviceUrl(client.getServiceUrl())
                .listenerThreads(client.getListenerThreads())
                .enableTransaction(client.isEnableTransaction())
                .maxLookupRequests(client.getMaxLookupRequests())
                .memoryLimit(client.getMemoryLimit(), SizeUnit.MEGA_BYTES)
                .statsInterval(client.getStatsInterval().toMillis(), TimeUnit.MILLISECONDS)
                .lookupTimeout((int) client.getLookupTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .operationTimeout((int) client.getOperationTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .connectionTimeout((int) client.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS);
        processAuthentication(builder, client);

        builderCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));

        Iterator<PulsarClientCustomizer> iterator = clientCustomizers.orderedStream().iterator();
        PulsarClient instance = builder.build();
        while (iterator.hasNext()) {
            PulsarClientCustomizer customizer = iterator.next();
            instance = customizer.customize(instance);
        }
        return instance;
    }

    private void processAuthentication(ClientBuilder builder, PulsarProperties.Client client) throws PulsarClientException.UnsupportedAuthenticationException {
        PulsarProperties.Authentication authentication = client.getAuthentication();
        PulsarProperties.Authentication.Oauth2 oauth2 = authentication.getOauth2();
        PulsarProperties.Authentication.AccessToken token = authentication.getToken();
        if (token.isEnabled()) {
            String payload = token.getPayload();
            Assert.isTrue(StringUtils.hasText(payload), "Missing pulsar.client.authentication.token.payload");
            builder.authentication(AuthenticationFactory.token(payload));
        } else if (oauth2.isEnabled()) {
            String clientId = oauth2.getClientId();
            String clientSecret = oauth2.getClientSecret();
            Assert.isTrue(StringUtils.hasText(clientId), "Missing pulsar.client.authentication.oauth2.clientId");
            Assert.isTrue(StringUtils.hasText(clientSecret), "Missing pulsar.client.authentication.oauth2.clientSecret");
            String payload = String.format("""
                    {"client_id":"%s","client_secret":"%s"}
                    """, clientId, clientSecret);
            String privateKey = "data:application/json;base64," + Base64.getEncoder().encodeToString(payload.getBytes(UTF_8));
            String issuerUrl = oauth2.getIssuerUrl();
            String audience = oauth2.getAudience();
            Assert.isTrue(StringUtils.hasText(issuerUrl), "Missing pulsar.client.authentication.oauth2.issuerUrl");
            Assert.isTrue(StringUtils.hasText(audience), "Missing pulsar.client.authentication.oauth2.audience");
            String accessToken = String.format("""
                    {"type":"client_credentials","privateKey":"%s","issuerUrl":"%s","audience":"%s"}
                    """, privateKey, issuerUrl, audience);
            Authentication auth = AuthenticationFactory
                    .create(AuthenticationOAuth2.class.getName(), accessToken);
            builder.authentication(auth);
        }
    }

    @Bean
    public MessageConverterRegister defaultMessageConverterRegister(List<MessageConverter> converters) {
        return new DefaultMessageConverterRegister(converters);
    }

}
