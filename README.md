# apache-pulsar-spring-boot-starter

> 基于Spring3.x，最低JDK版本要求JDK17

Apache pulsar与spring boot的集成.

1. [连接配置](#一连接配置)
    - [Pulsar Admin](#11-配置pulsar-admin)
    - [Pulsar Client](#12-配置pulsar-client)

## 一、连接配置

#### 1.1 配置Pulsar Admin

当配置了`pulsar.admin.service-http-url`之后，就会装配`PulsarAdmin`实例.

```yaml
pulsar:
  admin:
    service-http-url: http://127.0.0.1:8080
    connection-timeout: 5s
    read-timeout: 20s
    request-timeout: 30s
```

如果想要在实例正式被创建之前自定义`PulsarAdmin`的配置，可以通过定义实现了`PulsarAdminBuilderCustomizer`接口的`Bean`操作

#### 1.2 配置Pulsar Client

```yaml
pulsar:
  client:
    service-url: pulsar://localhost:6650
    io-threads: 8 # 默认为CPU核心数
    listener-threads: 8 # 默认为CPU核心数
    authentication:
      token:
        enabled: true # true表示启用token认证
        payload: xxxx
      oauth2:
        enabled: true # true表示启用oauth2认证.当token也启用时会优先使用Token认证
        audience: xxx
        client-id: xxx
        client-secret: xxx
        issuer-url: xxx
```

如果想要在实例正式被创建之前自定义`PulsarClient`的配置，可以通过定义实现了`PulsarClientBuilderCustomizer`接口的`Bean`处理.

如果在`PulsarClient`实例化之后仍然想要对`PulsarClient`实例进行定制操作，可以通过定义实现了`PulsarClientCustomizer`
接口的`Bean`处理.
