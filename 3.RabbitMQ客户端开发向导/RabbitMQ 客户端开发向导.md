---
title: RabbitMQ 客户端开发向导
date: 2019-09-21 15:37
toc: true
tag: rabbit-mq
thumbnail: http://img.yuzh.xyz/20190921153731_iYJOZ9_tj-sedisa-3K9XZD3cpis-unsplash.jpeg
---

<h1>第三章：RabbitMQ 客户端开发向导</h1>
<h2>Table Of Contents</h2>
<!-- TOC -->
- [连接 RabbitMQ](#%E8%BF%9E%E6%8E%A5-rabbitmq)
- [使用交换器和队列](#%E4%BD%BF%E7%94%A8%E4%BA%A4%E6%8D%A2%E5%99%A8%E5%92%8C%E9%98%9F%E5%88%97)
    - [exchangeDeclare 方法](#exchangedeclare-%E6%96%B9%E6%B3%95)
    - [queueDeclare 方法](#queuedeclare-%E6%96%B9%E6%B3%95)
    - [queueBind 方法](#queuebind-%E6%96%B9%E6%B3%95)
    - [exchangeBind 方法](#exchangebind-%E6%96%B9%E6%B3%95)
    - [何时创建](#%E4%BD%95%E6%97%B6%E5%88%9B%E5%BB%BA)
- [发送消息](#%E5%8F%91%E9%80%81%E6%B6%88%E6%81%AF)
- [消费消息](#%E6%B6%88%E8%B4%B9%E6%B6%88%E6%81%AF)
    - [推模式](#%E6%8E%A8%E6%A8%A1%E5%BC%8F)
    - [拉模式](#%E6%8B%89%E6%A8%A1%E5%BC%8F)
- [消费端的确认与拒绝](#%E6%B6%88%E8%B4%B9%E7%AB%AF%E7%9A%84%E7%A1%AE%E8%AE%A4%E4%B8%8E%E6%8B%92%E7%BB%9D)
- [关闭连接](#%E5%85%B3%E9%97%AD%E8%BF%9E%E6%8E%A5)
- [小结](#%E5%B0%8F%E7%BB%93)

<!-- /TOC -->

RabbitMQ Java 客户端使用 com.rabbitmq.client 作为顶级包名。关键的 class 和 interface 有：`Channel、Connection、ConnectionFactory、Consumer`等。

AMQP 协议层面的操作通过 Channel 接口实现，与 RabbitMQ 相关的开发工作，基本上都是围绕 `Connection` 和 `Channel` 这两个类展开的。

<!-- more -->
## 连接 RabbitMQ

```xml
<dependency>
   <groupId>com.rabbitmq</groupId>
   <artifactId>amqp-client</artifactId>
   <version>4.2.1</version>
</dependency>
```

创建连接的方法是：

```Java
public static void main(String[] args) throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    // rabbitmq 默认虚拟机名称为“/”，虚拟机相当于一个独立的mq服务器
    factory.setVirtualHost("/");
    factory.setUsername("root");
    factory.setPassword("root");
    factory.setHost("127.0.0.1");
    factory.setPort(5672);

    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
}
```

注意：Connection 可以用来创建多个 Channel，但是 Channel 示例不能在多个线程间共享，应用程序应该为每一个线程开辟一个 Channel。**多线程共享 Channel 实例是非线程安全的！**

## 使用交换器和队列
交换器和队列是 AMQP 协议中 high-level 层面的模块，应用程序需要确保使用他们的时候就已经存在了。所以记得使用之前先声明（Declare）。

声明交换器和队列的代码如下：

```Java
channel.exchangeDeclare(EXCHANGE, "direct", true, false, false, null);
channel.queueDeclare(QUEUE, true, false, false, null);
channel.queueBind(QUEUE, EXCHANGE, BINDING_KEY, null);
```

### exchangeDeclare 方法
exchangeDeclare 有多个重载方法，这些重载方法都是由下面这个方法中的缺省参数构成的。

```java
Exchange.DeclareOk exchangeDeclare(String exchange,
                                  String type,
                                  boolean durable,
                                  boolean autoDelete,
                                  boolean internal,
                                  Map<String, Object> arguments) throws IOException;
```

方法返回值 Exchange.DeclareOk，用于标示成功声明了一个交换器。

- exchange：交换器名称
- type：交换器类型
- durable：是否持久化，持久化可以将交换器存盘，服务器重启后不会丢失相关信息。
- autoDelete：是否自动删除，当最后一个交换器或队列与该交换器 unbind 之后自动删除。
- internal：是否是内置的。标记为内置的交换器客户端无法直接发送消息到该交换器，只能通过交换器路由到该交换器。
- arguments：其他的一些结构化参数。

与 exchangeDeclare 类似的方法还有 `exchangeDeclareNoWait` 和 `exchangeDeclarePassive`。exchangeDeclareNoWait 一般不建议使用，exchangeDeclarePassive 用来在声明交换器时检测是否已存在，如果已存在则返回，如果不存在则抛出异常：404 channel exception，同时 Channel 也会被关闭。

**删除交换器的方法：**

```Java
Exchange.DeleteOk exchangeDelete(String exchange, boolean ifUnused) throws IOException;

void exchangeDeleteNoWait(String exchange, boolean ifUnused) throws IOException;
```

如果 isUnused 设置为 true，则只有在此交换器没有被使用的情况下才会被删除；如果设置 false，则无论如何这个交换器都要被删除。

### queueDeclare 方法

```Java
// 1
Queue.DeclareOk queueDeclare() throws IOException;
// 2
Queue.DeclareOk queueDeclare(String queue,
                            boolean durable,
                            boolean exclusive,
                            boolean autoDelete,
                            Map<String, Object> arguments) throws IOException;
```

不带参数的 queueDeclare 默认一个由 RabbitMQ 命令的、排他的、自动删除的、非持久化的队列。

- `queue`：队列名称
- `durable`：是否持久化
- `exclusive`：是否为排他队列。如果一个队列是排他队列，该队列仅对「首次」声明它的 Connection 可用，并在连接断开时自动删除。
  1. 排他队列是基于 Connection 可见的，所以同一个 Connection 创建的不同 Channel 是可以同时访问同一连接创建的排他队列的。
  2. 「首次」是指一个 Connection 声明了一个排他队列，其他连接不能创建同名的排他队列了。
  3. 即使该队列是持久化的，一旦连接关闭或者客户端退出，该排他队列都会被自动删除。
  4. 这种队列适用于一个客户端同时发送和读取消息的应用场景。
- `autoDelete`：是否自动删除，最后一个客户端断开时删除。
- `arguments`：设置队列的一些其他参数。

> 注意：生产者和消费者都能够使用 queueDeclare 来声明一个队列，但是如果消费者在同一个 信道上订阅了另一个队列，就无法再声明队列了。必须先取消订阅，然后将信道置为"传输"模式，之后才能声明队列。

与 queueDeclare 类似也有 `queueDeclareNoWait` 和 `queueDeclarePassive` 方法。queueDeclarePassive 会检测队列是否存在，存在则返回，不存在则抛出异常。

**删除队列的方法：**

```Java
Queue.DeleteOk queueDelete(String queue, boolean ifUnused, boolean ifEmpty) throws IOException;

void queueDeleteNoWait(String queue, boolean ifUnused, boolean ifEmpty) throws IOException;
```

ifUnused 为 true 则在队列没有使用时才删除，ifEmpty 为 true 则在队列中没有任何消息时才能删除。

**清空队列的方法：**

```Java
Queue.PurgeOk queuePurge(String queue) throws IOException;
```

### queueBind 方法

```Java
Queue.BindOk queueBind(String queue, String exchange, String routingKey, Map<String, Object> arguments) throws IOException;
```

解绑方法：

```Java
Queue.UnbindOk queueUnbind(String queue, String exchange, String routingKey, Map<String, Object> arguments) throws IOException;
```

### exchangeBind 方法
不仅可以将队列与交换器绑定，还可以将交换器与交换器绑定，方法如下：

```Java
Exchange.BindOk exchangeBind(String destination, String source, String routingKey) throws IOException;

Exchange.BindOk exchangeBind(String destination, String source, String routingKey, Map<String, Object> arguments) throws IOException;

void exchangeBindNoWait(String destination, String source, String routingKey, Map<String, Object> arguments) throws IOException;
```

绑定之后消息发送到 source 交换器，source 交换器再路由给 destination 交换器，再由 destination 交换器路由到队列。

![](http://img.yuzh.xyz/20190921170250_X0p7el_Screenshot.png)

## 发送消息

```Java
void basicPublish(String exchange, String routingKey, BasicProperties props, byte[] body) throws IOException;

void basicPublish(String exchange, String routingKey, boolean mandatory, BasicProperties props, byte[] body) throws IOException;

void basicPublish(String exchange, String routingKey, boolean mandatory, boolean immediate, BasicProperties props, byte[] body) throws IOException;
```

- `exchange`：交换器名称，如果为空串，则消息会被发送到 RabbitMQ 默认交换器中。
- `routingKey`：路由键，根据路由键将消息存储到指定队列。
- `props`：消息的基本属性集，其包含 14 个属性成员，分别有：
    - contentType
    - contentEncoding
    - headers (Map<String ， Object>)
    - deliveryMode
    - priority
    - correlationld
    - replyTo
    - expiration
    - messageld
    - timestamp
    - type
    - userld
    - appld
    - clusterld
- `body`：消息体
- `mandatory` 和 `immediate`：见后续知识点

发送一条消息，自定义以下属性：

- 投递模式 (delivery mode) 设置为 2，即：消息会被持久化在服务器中。
- 这条消息的优先级 (priority)设置为 1。
- content-type 为 "text/plain"。
- 带有 headers。
- 带有过期时间。

```Java
byte[] body = "Hello Word!".getBytes();
// channel.basicPublish(EXCHANGE, ROUTING_KEY, true, MessageProperties.PERSISTENT_TEXT_PLAIN, body);

Map<String, Object> headers = new HashMap<>();
headers.put("location", "here");
headers.put("time ", "today");

channel.basicPublish(
        EXCHANGE,
        ROUTING_KEY,
        new AMQP.BasicProperties().builder()
                .contentType("text/plain")
                .deliveryMode(2)
                .priority(1)
                .userId("hidden")
                .headers(headers)
                .expiration("60000")
                .build(),
        body);
```

## 消费消息
RabbitMQ 消费模式分为「推」和「拉」两种模式。推模式采用 Basic.Consume 消费，拉模式采用 Basic.Get 消费。

### 推模式
推模式中主要使用订阅方式消费消息，接收消息一般通过实现 Comsumer、DefaultConsumer 类来实现，不同的订阅可以通过标签来区分。下面是消费消息的部分代码：

```Java
channel.basicQos(64);
channel.basicConsume(QUEUE, false, "consumerTag", true, false, null,
        new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String routingKey = envelope.getRoutingKey();
                String contentType = properties.getContentType();
                long deliveryTag = envelope.getDeliveryTag();
                // ...... 业务逻辑
                System.out.println(new String(body));
                channel.basicAck(deliveryTag, false);
            }
        });
```

主要 API：

```Java
String basicConsume(String queue, boolean autoAck, String consumerTag, boolean noLocal, boolean exclusive, Map<String, Object> arguments, Consumer callback) throws IOException;
```

- queue：队列的名称
- autoAck：设置是否自动确认，建议为 false。
- consumerTag：消费者标签，用来区分多个消费者。
- noLocal：设置为 true 表示不能将同一个 Connection 中的生产者消息发送给这个 Connection 的消费者。
- exclusive：是否排他
- arguments：其他 AMQP 参数
- callback：回调函数，用来处理 RabbitMQ 推送过来的消息。


**处理消息的回调方法：**

```java
public void handleDelivery(String consumerTag,
                           Envelope envelope,
                           AMQP.BasicProperties properties,
                           byte[] body)
```

**当 Connection 或 Channel 关闭时调用**

```java
public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig)
```

**在其他方法之前调用，返回消费者标签**

```Java
public void handleConsumeOk(String consumerTag)
```

**隐式或显式取消订阅时调用**

```Java
public void handleCancel(String consumerTag);
public void handleCancelOk(String consumerTag);
```

**取消一个消费者的订阅**

```Java
void basicCancel(String consumerTag) throws IOException;
```

> 使用消费者同样需要注意线程安全问题，消费者客户端上的各种 callback 方法都会分配到与 Channel 不同的线程之上去。每个 Channel 都有自己独立的线程，建议一个消费者使用一个 Channel。

### 拉模式
拉模式主要通过 channel.basicGet() 方法单条的获取消息，返回值是 GetResponse。

获得单条消息：

```Java
GetResponse response = channel.basicGet(QUEUE, false);
System.out.println(new String(response.getBody()));
// 确认消息已被接收
channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
```

## 消费端的确认与拒绝
### 确认（basic.ack）
为了保证消息可靠达到消费者，RabbitMQ 提供了消息确认机制（message acknowledgement）。消费者在订阅消息时通过指定 `autoAck` 参数。

当 autoAck 参数为 false，MQ 会等待消费者显式地回复确认信号后才会从内存（或磁盘）中移去消息；当 autoAck 为 true 时，MQ 会自动把发送出去的消息置为确认，然后从内存中移除，不管消费者是否接到了这个消息。

设置了 autoAck 为 false，MQ 会一直等待持有消息的消费者显式地调用 Bacis.Ack 命令为止。如果消息发送出去之后**消费端断开了连接**没有回调 basic.ack ，该消息会重新入队，等待投递给下一个消费者。

> RabbitMQ 不会为未确认的消息设置过期时间，它判断消息是否需要重新投递的依据是消费该消息的消费者是否断开连接。

查看队列中的消息状态：

```sh
# name messages_ready messages_unacknowledged 是指定要查看的属性名
~ » rabbitmqctl list_queues name messages_ready messages_unacknowledged                        Harry@192
Timeout: 60.0 seconds ...
Listing queues for vhost / ...
name	messages_ready	messages_unacknowledged
queue_demo	0	0
queueDemo	0	0
---------------------------------------------------------------------------------------------------------
```

### 拒绝（basic.reject）
RabbitMQ 2.0 后引入了 basic.reject 命令，客户端对此包装的方法是：

```Java
void basicReject(long deliveryTag, boolean requeue) throws IOException;
```

- deliveryTag：消息编号
- requeue：消息是否重新入队

basic.reject 一次只能拒绝一条消息，如果想要批量拒绝消息可以使用 bacis.nack，客户端对此包装的方法是：

```Java
void basicNack(long deliveryTag, boolean multiple, boolean requeue) throws IOException;
```

- multiple：如果为 false 表示拒绝编号为 deliveryTag 的消息，这于 Reject 的作用一样；如果为 true 表示拒绝 deliveryTag 之前的未被当前消费者确认的消息。

### 重新入队（basic.recover）
默认为 ack 的消息只有当消费端断开连接之后才会重新入队，使用 bacis.recover 可以请求 RabbitMQ 重新发送未被确认的消息，其客户端实现是：

```Java
Basic.RecoverOk basicRecover() throws IOException;
Basic.RecoverOk basicRecover(boolean requeue) throws IOException;
```

- requeue：如果为 false，重新发送的消息会发送上一个相同的消费者，如果为 true，则重新发送的消息有可能会发送与上一个消费者不同的消费者。

## 关闭连接

```Java
channel.close();
connection.close();
```

## 小结
消息生产者完整代码案例：

```Java
public class Producer {

    private static final String EXCHANGE = "exchangeDemo";
    private static final String QUEUE = "queueDemo";
    private static final String BINDING_KEY = "routingKeyDemo";
    private static final String ROUTING_KEY = "routingKeyDemo";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        // rabbitmq 默认虚拟机名称为“/”，虚拟机相当于一个独立的mq服务器
        factory.setVirtualHost("/");
        factory.setUsername("root");
        factory.setPassword("root");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE, "direct", true, false, false, null);
        channel.queueDeclare(QUEUE, true, false, false, null);
        channel.queueBind(QUEUE, EXCHANGE, BINDING_KEY, null);

        String content = "Hello Word! current: " + LocalDateTime.now();
        byte[] body = content.getBytes();
        // channel.basicPublish(EXCHANGE, ROUTING_KEY, true, MessageProperties.PERSISTENT_TEXT_PLAIN, body);

        Map<String, Object> headers = new HashMap<>();
        headers.put("location", "here");
        headers.put("time ", "today");
        channel.basicPublish(
                EXCHANGE,
                ROUTING_KEY,
                new AMQP.BasicProperties().builder()
                        .contentType("text/plain")
                        .deliveryMode(2)
                        .priority(1)
                        .userId("root")
                        .headers(headers)
                        .expiration("60000")
                        .build(),
                body);

        channel.close();
        connection.close();
    }

}
```

消息消费者完整代码案例：

```Java
public class Consumer {

    private static final String QUEUE = "queueDemo";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setVirtualHost("/");
        factory.setUsername("root");
        factory.setPassword("root");
        Address[] addresses = {
                new Address("127.0.0.1", 5672)
        };
        Connection connection = factory.newConnection(addresses);
        Channel channel = connection.createChannel();
        channel.basicQos(64);
        /*
          持续订阅消息
         */
        channel.basicConsume(QUEUE, false, "consumerTag", true, false, null,
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                               byte[] body) throws IOException {
                        String routingKey = envelope.getRoutingKey();
                        String contentType = properties.getContentType();
                        long deliveryTag = envelope.getDeliveryTag();
                        // ...... 业务逻辑
                        System.out.println(new String(body));
                        channel.basicAck(deliveryTag, false);
                    }
                });

        /*
          获得单条消息
         */
        // GetResponse response = channel.basicGet(QUEUE, false);
        // System.out.println(new String(response.getBody()));
        // channel.basicAck(response.getEnvelope().getDeliveryTag(), false);

//        channel.close();
//        connection.close();
    }

}
```
