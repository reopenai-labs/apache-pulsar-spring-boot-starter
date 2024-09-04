# apache-pulsar-spring-boot-starter

> 基于Spring3.x，最低JDK版本要求JDK17

Apache pulsar与spring boot的集成.一个高性能，轻量级，高度可定制化的框架。

此框架依赖非常干净，仅仅只依赖了`pulsar-client`以及`spring-boot`依赖. 内部组件设计的非常灵活，可以按照需求随意定制以及扩展框架的能力。

1. [连接配置](#一连接配置)
    - [Pulsar Admin](#11-配置pulsar-admin)
    - [Pulsar Client](#12-配置pulsar-client)
2. [发送消息](#二发送消息)
    - [简单示例](#21-简单示例)
    - [选择序列化协议发送消息](#22-同步异步的方式发送消息)
    - [发送延迟消息](#23-发送延迟消息)
    - [消息属性](#24-消息属性)
    - [指定消息的Key](#25-指定消息的key)
    - [其他能力](#26-其他能力)
    - [自定义序列化协议](#27-自定义序列化协议)
    - [自定义自己的参数解析器注解](#28-自定义自己的参数解析器注解)
    - [自定义自己的方法处理器注解](#29-自定义自己的方法处理器注解)
  

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

## 二、发送消息

创建一个生产者再发送消息在此框架中是一件非常非常非常简单的事情.

#### 2.1 简单示例

```java
import com.reopenai.component.pulsar.annotation.MessageParam;
import com.reopenai.component.pulsar.annotation.MessageValue;
import com.reopenai.component.pulsar.producer.annotation.ProducerMethod;
import com.reopenai.component.pulsar.producer.annotation.PulsarProducer;

// 在接口上声明Producer的配置即可创建一个producer实例.
// 更多详细配置请参考此注解的值
@PulsarProducer(
        tenant = "${application.pulsar.tenant:public}", // 支持SpEL表达式
        namespace = "default",
        producerName = "my-topic"
)
public interface MyProducer {

    // 在方法上添加一个@ProducerMethod注解，然后调用此方法，消息就能够发送出去了
    @ProducerMethod
    void sendMessage1(String message);

    // 当有一个以上的参数时，需要为消息内容添加一个@MessageValue的注解，表示此参数为消息荷载
    @ProducerMethod
    void sendMessage2(@MessageParam("key") String property,
                      @MessageValue MyMessage message);

}
```

#### 2.2 同步/异步的方式发送消息

```java
import com.reopenai.component.pulsar.producer.annotation.ProducerMethod;
import com.reopenai.component.pulsar.producer.annotation.PulsarProducer;
import org.apache.pulsar.client.api.MessageId;

import java.util.concurrent.CompletableFuture;

@PulsarProducer(
        tenant = "public",
        namespace = "default",
        producerName = "my-topic"
)
public interface MyProducer {

    // 当返回值定义为void时，将会以同步的方式发送消息
    @ProducerMethod
    void syncSendMessage1(String message);

    // 当返回值定义为MessageId时，将会以同步的方式发送消息
    @ProducerMethod
    MessageId syncSendMessage2(String message);

    // 当返回值定义为CompletableFuture<MessageId>时，将会以异步的方式发送消息
    @ProducerMethod
    CompletableFuture<MessageId> asyncSendMessage(String message);

    // 需要注意的是，默认情况下，被@ProducerMethod标记的方法的返回值只支持void/MessageId/CompletableFuture<MessageId>

}

```

#### 2.2 选择序列化协议发送消息

```java
import com.reopenai.component.pulsar.constant.MessageProtocol;
import com.reopenai.component.pulsar.producer.annotation.ProducerMethod;
import com.reopenai.component.pulsar.producer.annotation.PulsarProducer;

@PulsarProducer(
        tenant = "public",
        namespace = "default",
        producerName = "my-topic"
)
public interface MyProducer {

    // 在ProducerMethod注解中指定序列化协议
    // 当前内置支持JDK/JSON/PROTOBUF/PROTOSTUFF
    // 默认序列化协议为JSON，当然你也可以定义自己的序列化协议
    // 后面的内容会讲如何自定义序列化协议
    @ProducerMethod(protocol = MessageProtocol.JSON)
    void sendJsonMessage(MyMessage message);

    // 此消息将会使用JDK默认的序列化方式序列化消息
    @ProducerMethod(protocol = MessageProtocol.JDK)
    void sendJdkMessage(MyMessage message);

}

```

#### 2.3 发送延迟消息

```java
import com.reopenai.component.pulsar.annotation.MessageValue;
import com.reopenai.component.pulsar.producer.annotation.DelayedDelivery;
import com.reopenai.component.pulsar.producer.annotation.ProducerMethod;
import com.reopenai.component.pulsar.producer.annotation.PulsarProducer;

import java.util.concurrent.TimeUnit;

@PulsarProducer(
        tenant = "public",
        namespace = "default",
        producerName = "my-topic"
)
public interface MyProducer {

    // 发送带有固定延迟1000ms的消息
    @ProducerMethod(
            delayed = @DelayedDelivery(1000)
    )
    void sendDelayedMessage1(MyMessage message);

    // 发送动态延迟时间的消息
    @ProducerMethod
    void sendDelayedMessage2(@DelayedDelivery int time,
                             @MessageValue MyMessage message);

    // 指定时间单位发送动态延迟消息
    @ProducerMethod
    void sendDelayedMessage3(@DelayedDelivery(timeUnit = TimeUnit.SECONDS) int time,
                             @MessageValue MyMessage message);
}
```

#### 2.4 消息属性

```java
import com.reopenai.component.pulsar.annotation.MessageParam;
import com.reopenai.component.pulsar.annotation.MessageParams;
import com.reopenai.component.pulsar.annotation.MessageProperty;
import com.reopenai.component.pulsar.annotation.MessageValue;
import com.reopenai.component.pulsar.producer.annotation.ProducerMethod;
import com.reopenai.component.pulsar.producer.annotation.PulsarProducer;

@PulsarProducer(
        tenant = "public",
        namespace = "default",
        producerName = "my-topic"
)
public interface MyProducer {

    // 发送的消息将会携带 key1=value1、key2=value2的属性
    @ProducerMethod(
            properties = {
                    @MessageProperty(key = "key1", value = "value1"),
                    @MessageProperty(key = "key2", value = "value2")
            }
    )
    void sendPropertyMessage1(MyMessage message);

    // 发送消息时动态指定消息的属性.其中属性的key为key1
    @ProducerMethod
    void sendPropertyMessage2(@MessageParam("key1") String value,
                              @MessageValue MyMessage message);

    // 发送消息时将properties的值全部填入消息属性中
    @ProducerMethod
    void sendDelayedMessage3(@MessageParams Map<String, String> properties,
                             @MessageValue MyMessage message);

    // 结合使用不会有任何的冲突，全部的属性将会按照对应的规则处理
    @ProducerMethod(
            properties = {
                    @MessageProperty(key = "key1", value = "value1"),
                    @MessageProperty(key = "key2", value = "value2")
            }
    )
    void sendPropertyMessage1(@MessageParam("key1") String value,
                              @MessageParams Map<String, String> properties,
                              @MessageValue MyMessage message);

}

```

#### 2.5 指定消息的Key

```java
import com.reopenai.component.pulsar.annotation.MessageKey;
import com.reopenai.component.pulsar.annotation.MessageValue;
import com.reopenai.component.pulsar.producer.annotation.ProducerMethod;
import com.reopenai.component.pulsar.producer.annotation.PulsarProducer;

import java.util.concurrent.TimeUnit;

@PulsarProducer(
        tenant = "public",
        namespace = "default",
        producerName = "my-topic"
)
public interface MyProducer {

    // 将字段标记为消息key即可
    @ProducerMethod
    void sendDelayedMessage1(@MessageValue MyMessage message,
                             @MessageKey String key);

}
```

#### 2.6 其他能力

方法参数中还支持其他的注解，在这里不做详细的介绍，具体可查看注解的注释

- @MessageEventTime 标记参数为消息的`eventTime`

#### 2.7 自定义序列化协议

你可能会注意到，在`@ProducerMethod`注解中，`protocol`字段类型为`String`类型.之所以不用枚举类型是为了方便能够自定义序列化协议。
如果你想要支持一种新的序列化类型，只需要在`Spring`上下文中注册一个实现了`MessageConverter`接口的`Bean`即可.

```java

import org.springframework.stereotype.Component;
import com.reopenai.component.pulsar.serialization.MessageConverter;

@Component
public class MyMessageConverter implements MessageConverter {

    @Override
    public byte[] serializer(Object message, MethodParameter parameterInfo) {
        //处理序列化逻辑
    }

    @Override
    public Object deserializer(byte[] message, MethodParameter parameterInfo) {
        // 处理反序列化逻辑
    }

    @Override
    public String supportType() {
        return "myProtocol";
    }
}

```

然后就可以像往常一样选择此序列化协议来序列化消息.

```java
import com.reopenai.component.pulsar.constant.MessageProtocol;
import com.reopenai.component.pulsar.producer.annotation.ProducerMethod;
import com.reopenai.component.pulsar.producer.annotation.PulsarProducer;

@PulsarProducer(
        tenant = "public",
        namespace = "default",
        producerName = "my-topic"
)
public interface MyProducer {

    @ProducerMethod(protocol = "myProtocol")
    void sendMessage(MyMessage message);

}
```

#### 2.8 自定义自己的参数解析器注解

如果内置的参数解析器你觉得还不够用，还支持自定义参数解析器.

定义自己的注解

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyParameter {
}
```

创建一个实现了`ProducerArgumentResolve`接口的对象，并将其注册到`Spring`上下文中

```java
import com.reopenai.component.pulsar.resolve.ProducerArgumentResolve;
import org.springframework.core.MethodParameter;

public class MyParameterProducerArgumentResolve implements ProducerArgumentResolve {

    @Override
    public boolean supportsParameter(MethodParameter parameterInfo) {
        // 判断该解析器是否能处理此参数
    }

    @Override
    public void resolveArgument(MethodParameter parameterInfo, Object value, ProducerMessage message) {
        // 处理参数解析的逻辑，并可以将解析后的结果设置到message中
    }

}

```

#### 2.9 自定义自己的方法处理器注解

什么？你觉的`@ProducerMethod`写的很糟糕还是不够用？或者你还想扩展它的能力？好吧满足你的要求...
你可以通过实现`ProducerMethodInvokerFactory`接口完成此功能。`@ProducerMethod`也是基于此实现的.
