package com.reopenai.component.pulsar.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author Allen Huang
 */
@Data
@ConfigurationProperties("pulsar")
public class PulsarProperties {

    /**
     * 客户端配置
     */
    private Client client = new Client();

    /**
     * pulsar admin配置
     */
    private Admin admin = new Admin();

    @Data
    public static class Admin {
        /**
         * 服务地址
         */
        private String serviceHttpUrl;
        /**
         * 请求的超时时间.默认为30s
         */
        private Duration requestTimeout = Duration.ofSeconds(30);
        /**
         * 连接超时时间.默认为5s
         */
        private Duration connectionTimeout = Duration.ofSeconds(5);
        /**
         * read timeout.默认为20s
         */
        private Duration readTimeout = Duration.ofSeconds(20);
    }

    @Data
    public static class Client {

        /**
         * 是否开启事物
         */
        private boolean enableTransaction;

        /**
         * Pulsar service URL in the format '(pulsar|pulsar+ssl)://host:port'.
         */
        private String serviceUrl = "pulsar://localhost:6650";

        /**
         * Client operation timeout.
         */
        private Duration operationTimeout = Duration.ofSeconds(30);

        /**
         * Client lookup timeout.
         */
        private Duration lookupTimeout;

        /**
         * Duration to wait for a connection to a broker to be established.
         */
        private Duration connectionTimeout = Duration.ofSeconds(10);

        /**
         * IO线程数.默认为CPU核心数
         */
        private int ioThreads = Runtime.getRuntime().availableProcessors();

        /**
         * 监听线程数.默认为CPU核心数
         */
        private int listenerThreads = Runtime.getRuntime().availableProcessors();

        /**
         * 每个Broker连接的最大请求数.默认值为5000
         */
        private int maxLookupRequests = 5000;

        /**
         * 直接内存大小限制,默认为64.单位MB
         */
        private int memoryLimit = 64;

        /**
         * 客户端统计的周期，单位毫秒，-1表示不统计。默认为不统计
         */
        private Duration statsInterval = Duration.ofSeconds(-1);

        /**
         * 认证配置
         */
        private Authentication authentication = new Authentication();

    }

    @Data
    public static class Authentication {
        /**
         * 通过Token的方式进行认证
         */
        private AccessToken token = new AccessToken();
        /**
         * 通过Oauth2的方式进行认证
         */
        private Oauth2 oauth2 = new Oauth2();

        @Data
        public static class AccessToken {
            /**
             * 是否启用
             */
            private boolean enabled;
            /**
             * 通过Token的方式进行认证
             */
            private String payload;
        }

        @Data
        public static class Oauth2 {
            /**
             * 是否启用
             */
            private boolean enabled;
            /**
             * issuerUrl
             */
            private String issuerUrl;
            /**
             * audience
             */
            private String audience;
            /**
             * client_id
             */
            private String clientId;
            /**
             * client_secret
             */
            private String clientSecret;
        }

    }
}
