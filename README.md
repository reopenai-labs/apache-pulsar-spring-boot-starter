# apache-pulsar-spring-boot-starter

> 基于 Spring Boot 4.x（Spring Framework 7），最低 JDK 17

Apache Pulsar 与 Spring Boot 的声明式集成框架。用接口/类 + 注解的方式定义 Producer 与 Consumer，同时提供高度可扩展的能力抽象。

- **声明式 Producer**：`@PulsarProducer` 接口 + `@ProducerMethod` 方法，自动代理实现
- **声明式 Consumer**：`@PulsarConsumer` 类 + `@ConsumerHandler` 方法，自动注册消费
- **能力插件化**：`ProducerCapability` + `ProducerCapabilityProvider`，继承接口即获得能力（动态 Topic 等）
- **生产级特性**：事务感知发送、消息处理器链、序列化协议契约、消息去重键、死信/重试
- **全自动装配**：Producer 默认扫描 main 包，零配置；PulsarClient 由 spring-boot-starter-pulsar 初始化

## 依赖

```xml
<dependency>
    <groupId>com.reopenai</groupId>
    <artifactId>apache-pulsar-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

底层客户端（PulsarClient/PulsarAdmin）由 `spring-boot-starter-pulsar` 负责初始化，按 `spring.pulsar.*` 配置即可。

---

## 一、Producer 声明式发送

定义一个接口，标注 `@PulsarProducer`，方法标注 `@ProducerMethod`，即可注入并发送消息。**默认无需任何配置**，框架自动扫描 main class 所在包下的所有 `@PulsarProducer` 接口。

```java
@PulsarProducer(tenant = "public", namespace = "default", topicName = "order")
public interface OrderProducer {

    // 返回 void / MessageId → 同步发送；CompletableFuture<MessageId> → 异步发送
    @ProducerMethod
    void send(Order order);

    @ProducerMethod
    CompletableFuture<MessageId> sendAsync(Order order);
}
```

仅当需要导入 main 包之外的 Producer 时，才用 `@ImportProducers` 显式指定扫描包：

```java
@ImportProducers("com.thirdparty.producer")
@SpringBootApplication
public class App { }
```

### 序列化协议

`@ProducerMethod(protocol = ...)` 选择序列化协议，内置 `JSON`（默认）/`JDK`，可通过实现 `MessageConverter` 扩展。**协议会写入消息属性**，Consumer 据此自动反序列化。

```java
@ProducerMethod(protocol = MessageProtocol.JDK)
void sendJdk(Order order);
```

### 消息属性 / Key / 延迟 / EventTime

```java
@ProducerMethod(
    messageKey = "order-key",                       // 消息 Key（同 key 保序）
    properties = { @MessageProperty(key = "k", value = "v") }
)
void send(@MessageValue Order order,
          @MessageParam("traceId") String traceId,  // 动态属性
          @MessageParams Map<String, String> props, // 全部属性
          @MessageKey String key,                   // 动态 Key
          @MessageEventTime long eventTime);        // 事件时间

// 延迟消息
@ProducerMethod(delayed = @DelayedDelivery(1000))   // 固定延迟 1s
void sendDelayed(Order order);

@ProducerMethod
void sendDelayed(@DelayedDelivery(timeUnit = TimeUnit.SECONDS) int delay, @MessageValue Order order);
```

### 事务感知发送（DB + 消息一致性）

配合 Spring `@Transactional`，`AFTER_COMMIT` 模式下消息在 DB 事务提交后才发送，回滚则不发送：

```java
@ProducerMethod(transaction = TransactionPhase.AFTER_COMMIT)
CompletableFuture<MessageId> sendAfterCommit(Order order);
```

> 返回值语义：**异步**方法立即返回 `CompletableFuture`（事务提交后完成、回滚则异常完成）；**同步**方法立即返回 `null`——此时消息尚未发送（须待事务提交），故同步 `AFTER_COMMIT` 方法的返回值无意义，不应依赖其拿到 `MessageId`。

### 消息去重键

```java
@ProducerMethod(autoUniqueKey = true)   // 自动写入 hostname+uuid 到消息属性
void sendWithDedup(Order order);
```

### 消息处理器（发送治理）

实现 `ProducerMessageHandler` 并注册为 Bean，即可在发送前后插入埋点/追踪/改写等横切逻辑（按 `Ordered` 顺序执行）：

```java
@Component
public class TracingHandler implements ProducerMessageHandler {
    @Override
    public void sendBefore(ProducerMessage message, ProducerContext ctx) {
        message.property("traceId", MDC.get("traceId"));
    }
}
```

---

## 二、Producer 能力扩展（核心特性）

框架的扩展机制类似 Spring Data Repository：**继承能力接口即获得能力，新增能力无需改核心**。

### 动态 Topic（一个接口多 Topic）

`DynamicTopicCapability` 提供运行时添加 Topic 的能力，`@ProducerSelect` 在发送时路由到指定 Topic：

```java
@PulsarProducer(tenant = "public", namespace = "default", topicName = "default-topic")
public interface MyProducer extends DynamicTopicCapability {

    @ProducerMethod
    void send(@ProducerSelect String alias, @MessageValue String msg);
}

// 使用
producer.addTopicByName("order", "order-topic");           // 动态注册 Topic
producer.addTopicByFullName("log", "persistent://public/default/log-topic");
producer.send("order", "hello");   // 路由到 order-topic
```

### 自定义能力

新增一个能力只需 **1 个接口 + 1 个 Provider**，核心装配零改动：

```java
// 1. 能力接口
public interface BatchCapability extends ProducerCapability {
    void sendBatch(List<?> messages);
}

// 2. 能力实现（注册为 Bean）
@Component
public class BatchCapabilityProvider implements ProducerCapabilityProvider<BatchCapability> {
    public Class<BatchCapability> capabilityType() { return BatchCapability.class; }
    public Map<Method, ProducerMethodInvoker> createInvokers(ProducerContext ctx) { /* ... */ }
}

// 3. 继承即获得能力
public interface MyProducer extends DynamicTopicCapability, BatchCapability { ... }
```

---

## 三、Consumer 声明式消费

定义一个 Spring Bean，标注 `@PulsarConsumer`，方法标注 `@ConsumerHandler`，框架自动创建 Pulsar 消费者并启动消费。

```java
@Component
@PulsarConsumer(topicNames = "persistent://public/default/order", subscriptionName = "order-sub")
public class OrderConsumer {

    @ConsumerHandler
    public void handle(@MessageValue Order order) {
        // 自动 ack：方法成功返回后 ack，抛异常则 nack 重试
    }
}
```

### 参数解析

| 注解 | 说明 |
|---|---|
| `@MessageValue` | 消息体（按协议反序列化） |
| `@RawMessageId` | 消息 ID（`MessageId` 类型） |
| `@MessageTopic` | Topic 名称（`String`） |
| `@MessageKey` | 消息 Key（`String`） |
| `@MessageParam("k")` | 单个消息属性（`String`） |
| `@MessageParams` | 全部消息属性（`Map<String,String>`） |
| `@MessageEventTime` | 事件时间（`long`） |
| `ConsumerContext` 参数 | 手动 ack 模式（见下） |

### 手动 ACK

方法声明 `ConsumerContext` 参数即切换为手动 ack 模式，由用户决定何时确认：

```java
@ConsumerHandler
public void handle(@MessageValue Order order, ConsumerContext ctx) {
    process(order);
    ctx.doAcknowledge();          // 成功 ack
    // ctx.doNegativeAcknowledge(); // 失败 nack 重试
    // ctx.doReconsumeLater(1, TimeUnit.SECONDS); // 延迟重试
}
```

### 属性匹配分发

一个 Consumer 类可以有多个 `@ConsumerHandler`，按消息属性匹配分发到不同方法：

```java
@ConsumerHandler(properties = { @MessageProperty(key = "type", value = "create") })
public void onCreate(@MessageValue Order order) { ... }

@ConsumerHandler(properties = { @MessageProperty(key = "type", value = "cancel") })
public void onCancel(@MessageValue Order order) { ... }
```

### 死信 / 重试 / 批量

```java
@PulsarConsumer(
    topicNames = "persistent://public/default/order",
    subscriptionName = "order-sub",
    enableRetry = true,
    deadLetterPolicy = @DeadLetterPolicyConfig(maxRedeliverCount = 5),
    enableBatch = true,
    batchReceivePolicy = @BatchReceivePolicyConfig(maxNumMessages = 200)
)
```

### 错误处理

```java
@ConsumerHandler(
    ignoreDeserializeFail = true,                    // 反序列化失败时忽略（ack 丢弃）
    noNegativeFor = { BusinessException.class }      // 这些异常视为消费成功（ack）
)
public void handle(@MessageValue Order order) { ... }
```

### 消息处理器（消费治理）

实现 `ConsumerMessageHandler` 并注册为 Bean，即可在消费前后插入埋点/追踪/指标等横切逻辑（按 `Ordered` 顺序执行，异常被隔离，不影响消费与 ack），与 producer 端 `ProducerMessageHandler` 对称：

```java
@Component
public class ConsumeTracingHandler implements ConsumerMessageHandler {
    @Override
    public void consumeBefore(ConsumerMessageRecord record, ConsumerContext ctx) {
        MDC.put("traceId", record.getProperty("traceId"));
    }
    @Override
    public void consumeAfter(ConsumerMessageRecord record, ConsumerContext ctx, Throwable error) {
        // 记录消费耗时/结果
        MDC.clear();
    }
}
```

---

## 四、自定义扩展

### 自定义序列化协议

实现 `MessageConverter`（`supportedType` 返回协议名）并注册为 Bean：

```java
@Component
public class ProtobufMessageConverter implements MessageConverter {
    public byte[] serialize(Object message, MethodParameter parameter) { /* ... */ }
    public Object deserialize(byte[] message, MethodParameter parameter) { /* ... */ }
    public String supportedType() { return "PROTOBUF"; }
}

// 使用
@ProducerMethod(protocol = "PROTOBUF")
void send(MyProto msg);
```

### 自定义参数解析器

Producer 侧实现 `ProducerArgumentResolver`，Consumer 侧实现 `ConsumerArgumentResolver`，注册为 Bean 即可扩展方法参数的解析能力。

---

## 设计总览

```
producer/
├── core/            核心层：装配引擎 + 调用骨架 + 能力抽象（不含具体能力）
│   ├── capability/  ProducerCapability / ProducerCapabilityProvider（能力 SPI）
│   ├── handler/     ProducerMessageHandler（发送前后处理器）
│   ├── invoker/     方法调用链（Invoker / Executor / Message + 事务装饰器）
│   ├── resolver/    Producer 参数解析器（ProducerArgumentResolver 及实现）
│   ├── ProducerConfiguration / ProducerContext / ProducerRegistry
│   └── PulsarProducerFactoryBean / PulsarProducerProxy / PulsarProducerRegistrar / TransactionPhase
└── extension/       扩展层：具体能力实现（可插拔）
    └── dynamic/     DynamicTopicCapability + @ProducerSelect 动态 Topic 路由

consumer/
└── core/            核心层（与 producer/core 对称）
    ├── handler/     ConsumerMessageHandler（消费前后处理器）
    ├── invoker/     ConsumerMethodInvoker / ConsumerMessageRecord（消息只读视图）
    ├── resolver/    Consumer 参数解析器（ConsumerArgumentResolver 及实现）
    ├── ConsumerConfiguration（注解配置）/ ConsumerRegistry（生命周期）/ ConsumerContext
    └── PulsarConsumerBeanPostProcessor（扫描入口）

annotation/          全部注解（@PulsarProducer / @PulsarConsumer / @ProducerMethod / @ConsumerHandler …）
serialization/       MessageConverter + Registry + 内置 JSON / JDK
support/             MessageHeaders（协议契约常量）+ 工具类
```

**核心稳定、能力插件化**——任何"继承接口获得能力"或"新发送机制"的需求，都走 `ProducerCapability` + `Provider` 这一条路，核心装配无需改动。
