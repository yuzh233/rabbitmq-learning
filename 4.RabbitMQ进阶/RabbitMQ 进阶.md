---
title: RabbitMQ 进阶
date: 2019-09-24 09:57
toc: true
tag: rabbit-mq
thumbnail: http://img.yuzh.xyz/20190924164555_7pvwjM_timothy-meinberg-dKpMUOG8ck4-unsplash.jpeg
---

<h1>第四章：RabbitMQ 进阶</h1>
<h2>Table Of Contents</h2>
<!-- TOC -->

- [消息回退机制](#%E6%B6%88%E6%81%AF%E5%9B%9E%E9%80%80%E6%9C%BA%E5%88%B6)
    - [mandatory 参数](#mandatory-%E5%8F%82%E6%95%B0)
    - [immediate 参数](#immediate-%E5%8F%82%E6%95%B0)
    - [备份交换器](#%E5%A4%87%E4%BB%BD%E4%BA%A4%E6%8D%A2%E5%99%A8)
        - [为什么备份交换器的类型要设置为 fanout？](#%E4%B8%BA%E4%BB%80%E4%B9%88%E5%A4%87%E4%BB%BD%E4%BA%A4%E6%8D%A2%E5%99%A8%E7%9A%84%E7%B1%BB%E5%9E%8B%E8%A6%81%E8%AE%BE%E7%BD%AE%E4%B8%BA-fanout)
        - [使用备份交换器的一些注意点](#%E4%BD%BF%E7%94%A8%E5%A4%87%E4%BB%BD%E4%BA%A4%E6%8D%A2%E5%99%A8%E7%9A%84%E4%B8%80%E4%BA%9B%E6%B3%A8%E6%84%8F%E7%82%B9)
- [过期时间（TTL）](#%E8%BF%87%E6%9C%9F%E6%97%B6%E9%97%B4ttl)
    - [设置消息的 TTL](#%E8%AE%BE%E7%BD%AE%E6%B6%88%E6%81%AF%E7%9A%84-ttl)
        - [全部消息的 TTL](#%E5%85%A8%E9%83%A8%E6%B6%88%E6%81%AF%E7%9A%84-ttl)
        - [单条消息的 TTL](#%E5%8D%95%E6%9D%A1%E6%B6%88%E6%81%AF%E7%9A%84-ttl)
    - [设置队列的 TTL](#%E8%AE%BE%E7%BD%AE%E9%98%9F%E5%88%97%E7%9A%84-ttl)
- [死信队列（DLX）](#%E6%AD%BB%E4%BF%A1%E9%98%9F%E5%88%97dlx)
- [延迟队列](#%E5%BB%B6%E8%BF%9F%E9%98%9F%E5%88%97)
- [优先级队列](#%E4%BC%98%E5%85%88%E7%BA%A7%E9%98%9F%E5%88%97)
- [RPC 实现](#rpc-%E5%AE%9E%E7%8E%B0)
- [持久化](#%E6%8C%81%E4%B9%85%E5%8C%96)
- [生产者确认](#%E7%94%9F%E4%BA%A7%E8%80%85%E7%A1%AE%E8%AE%A4)
    - [事物机制](#%E4%BA%8B%E7%89%A9%E6%9C%BA%E5%88%B6)
    - [发送方确认机制](#%E5%8F%91%E9%80%81%E6%96%B9%E7%A1%AE%E8%AE%A4%E6%9C%BA%E5%88%B6)
- [消费端要点介绍](#%E6%B6%88%E8%B4%B9%E7%AB%AF%E8%A6%81%E7%82%B9%E4%BB%8B%E7%BB%8D)
    - [消息分发](#%E6%B6%88%E6%81%AF%E5%88%86%E5%8F%91)
    - [消息顺序性](#%E6%B6%88%E6%81%AF%E9%A1%BA%E5%BA%8F%E6%80%A7)
    - [启用 QueueingConsumer](#%E5%90%AF%E7%94%A8-queueingconsumer)
- [消息传输保障](#%E6%B6%88%E6%81%AF%E4%BC%A0%E8%BE%93%E4%BF%9D%E9%9A%9C)
- [小结](#%E5%B0%8F%E7%BB%93)

<!-- /TOC -->
<!-- more -->

## 消息回退机制
### mandatory 参数
mandatory 和 immediate 是 channel.basicPublish 中的两个参数，他们都有当消息传递过程不可达到目的地时将消息返回给生产者的功能。RabbitMQ 的 **备份交换器** 可以将未能被交换器路由的消息存储起来，而不用返回给客户端。

当 mandatory 参数设置为 true 时，交换器如果无法根据自身的类型和路由键在找到一个队列，那么 RabbitMQ 会调用 Bacis.Return 命令将消息返回给消费者。当 mandatory 参数为 false 时，则消息会被丢弃。

在客户端中，可以通过调用 `channel.addReturnListener` 来添加 ReturnListner 监听器来实现，具体实例代码如下：

```java
String content = "收到一条消息：" + LocalDateTime.now();
        channel.basicPublish(
                "demoExchange",
                "demoRoutingKey",
                // mandatory 参数为 true，消息不可达时将返回给生产者
                true,
                // immediate 参数不建议使用，一般为 false 即可。
                false,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                content.getBytes());

        // 添加监听代码
        channel.addReturnListener(new ReturnListener() {
            @Override
            public void handleReturn(int replyCode, String replyText, String exchange, String routingKey,
                                     AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("消息返回生产者，内容是：" + new String(body));
            }
        });
```

_模拟消息方法失败，将路由键置为空，消息不可达被原路返回：_

![](http://img.yuzh.xyz/20190924150342_Pndm36_Screenshot.png)

### immediate 参数
mandatory 参数作用是告诉服务器，如果消息没有路由到任何一个队列，则返回消息；

而 immediate 参数作用是：如果匹配的队列上没有一个消费者订阅，则返回消息，不用将消息存入队列而等待消费者。RabbitMQ 3.0 取消了对 immediate 参数的支持，建议采用 `TTL`（过期时间） 和 `DLX`（死信队列） 的方式替代。

### 备份交换器
备份交换器（Alternate Exchange），简称 AE，直白一点称作「备胎交换器」更加容易理解。

生产者发送消息的时候如果不指定 mandatory 参数为 true，那么消息未被路由的情况下会被丢失。添加了 mandatory 参数还需要添加 ReturnLisner 监听器。

如果不想添加额外的监听器，又不想消息丢失，可以使用备份交换器，它能将未被路由的消息存储在 RabbitMQ 中，再在需要的时候去处理。

使用备份交换器可通过在声明交换器时添加 `alternate-exchange` 参数来实现，也可以通过策略方式（暂时不管）实现，如果两者都存在则前者优先级更高。

实例代码：

```java
// 创建备份交换器和备份队列，使其绑定。注意：类型为fanout，即 "把消息路由到所有与之绑定的队列上去，忽略路由键。"
channel.exchangeDeclare("alternateExchange", "fanout", true, false, false, null);
channel.queueDeclare("alternateQueue", true, false, false, null);
channel.queueBind("alternateQueue", "alternateExchange", "alternateRoutingKey", null);

// 路由键参数, alternate-exchange 是固定参数
Map<String, Object> map = new HashMap<>();
map.put("alternate-exchange", "alternateExchange");

// 创建正常交换器和正常队列，使其绑定。并将 alternateExchange 作为该交换器的备份交换器。
channel.exchangeDeclare("normalExchange", "direct", true, false, false, map);
channel.queueDeclare("normalQueue", true, false, false, null);
channel.queueBind("normalQueue", "normalExchange", "demoRoutingKey", null);

String content = "收到一条消息：" + LocalDateTime.now();
channel.basicPublish(
                "normalExchange",
                "normalRoutingKey111",
                // mandatory 参数已经无效了，设置了 AE 消息就不会返回了。
                true,
                false,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                content.getBytes());

```

以上代码做了几件事：

- 声明了两个交换器 normalExchange 和 alternateExchange。
- 声明了两个队列 normalQueue 和 alternameQueue。
- 将两个交换器分别绑定对于的队列。
- 将 alternateExchange 作为 normalExchange 的备份交换器。

此时如果发送一条消息到 normalExchange 上，当路由键为 normalRoutingKey，消息能正确路由到 normalQueue 队列上；而当路由键为 normalRoutingKey111 时，消息不能正确地路由到与 normalExchange 绑定的 normalQueue 上，此时会发送到 alternateExchange 上，从而发送到 alternameQueue 这个队列。

![](http://img.yuzh.xyz/20190924160830_ixBsZs_Screenshot.png)

验证：创建两个消费者，分别订阅 alternateQueue 和 normalQueue：

```java
public class NormalConsumer {
    public static void main(String[] args) throws IOException, TimeoutException {
        ......
        channel.basicConsume(
                        "normalQueue",
                        ......);
    }
}

----------------------------------------------------------------------------------
public class AlternateConsumer {
    public static void main(String[] args) throws IOException, TimeoutException {
        ......
        channel.basicConsume(
                        "alternateQueue",
                        ......);
    }
}
```

#### 为什么备份交换器的类型要设置为 fanout？
是为了避免消息丢失，消息转发到备份交换器时的路由键是和生产者发出的路由键一样的。

试想如果备份交换器类型是 direct（路由键和绑定键一致才会入队），假设绑定键为 key1。

- 如果携带路由键 key2 的消息被转发到该备份交换器时，备份交换器没有找到合适的队列，此时消息会被丢弃。
- 如果路由键为 key1，则刚好可以存储到队列。

所以稳妥的办法是将备份交换器的类型设置为 fanout，只要有消息到了 AE，就会存储到所有与之绑定的队列中去从而避免消息丢失。

#### 使用备份交换器的一些注意点
- 如果 AE 不存在，客户端和 MQ 不会有异常出现，此时消息会丢失。
- 如果 AE 没有绑定任何队列，客户端和 MQ 不会有异常出现，此时消息会丢失。
- 如果 AE 没有匹配任何队列，客户端和 MQ 不会有异常出现，此时消息会丢失。
- 如果 AE 和 mandatory 参数一起使用，mandatory 参数将会无效。

## 过期时间（TTL）
TTL，全称 Time To Live，即过期时间。RabbitMQ 可以设置队列和消息的过期时间。

### 设置消息的 TTL
有两种方式可以设置消息的过期时间，一种是通过队列属性设置，队列中的所有消息都有相同的过期时间；一种是对消息本身进行单独设置，每条消息的 TTL 可以不同。

如果两者都定义了，则以数值小的那个为准。消息如果过期，会变成「死信」，消费者无法再次收到该消息（不是绝对的，见后续知识点。）

#### 全部消息的 TTL
通过队列属性设置消息 TTL 的方法是在 channel.queueDeclare 中指定 `x-message-ttl` 参数实现的，单位毫秒。

```java
channel.exchangeDeclare("demoExchange", "direct", true, false, false, null);

Map<String, Object> map = new HashMap<>();
map.put("x-message-ttl", 6000);

// 设置整个队列中消息的过期时间
channel.queueDeclare("demoQueue", true, false, false, map);
channel.queueBind("demoQueue", "demoExchange", "demoRoutingKey", null);
```

> 注意：执行代码如果出现报错 “inequivalent arg 'x-message-ttl' for queue” 那是因为该队列已经创建了，设置 ttl 等于重新创建了一个重名的队列。
> 测试情况下可以执行删除命令：**rabbitmqctl delete_queue 队列名**

还可以通过 Policy 和 HTTP 方式设置 TTL，这里不再深入。

> 如果 TTL = 0，则表示除非此时可以直接将消息投递给消费者，否则该消息会被丢弃。这个特性一定在部分上代替了 immediate 属性，之所以部分代替是因为 immdiate 属性在投递失败时会将消息返回。

#### 单条消息的 TTL
针对每条消息设置 TTL 的方法是在 channel.basicPublish 方法中加入 `expiration` 的属性参数，单位为毫秒。

```java
String content = "收到一条消息：" + LocalDateTime.now();
channel.basicPublish(
        "demoExchange",
        "demoRoutingKey",
        true,
        false,
        new AMQP.BasicProperties()
                .builder()
                // 消息持久化
                .deliveryMode(2)
                // TTL
                .expiration("6000")
                .build(),
        content.getBytes());

```

### 设置队列的 TTL
通过 channel.queueDeclare 方法中的 `x-expires` 参数可以控制队列被删除前未使用状态的时间。未使用状态是指队列上没有任何消费者，也没有被重新声明，过期时间段内也没有调用过 basic.get 命令。

x-expires 也是以毫秒为单位，不能设置为0，一下是实例代码：

```java
Map<String, Object> map = new HashMap<>();
// 设置队列的过期时间
map.put("x-expires",1800000);
channel.queueDeclare("demoQueue", true, false, false, map);
```

## 死信队列（DLX）
DLX，全称为 Dead-Letter-Exchange，也可以称为死信交换器。当消息在队列中变为死信之后，它能重新被发送到一个交换器中，这个交换器就是 DLX，绑定 DLX 的队列被称之为「死信队列」。

    可能把死信队列认为是 DLX 绑定的队列会好理解一点，一般称呼的时候就把 DLX 叫做死信队列，其实不然。

消息变为死信一般是由于一下几种情况：

- 消息被拒绝（Basic.Reject / Basic.Nack），并且设置了 requeue 为 false；
- 消息过期；
- 队列达到最大长度。

DLX 跟一般交换器一样是个正常的交换器，它通过设置某个正常队列的属性，使其成为该队列的 DLX。当该队列存在死信时，RabbitMQ 会自动将这个消息重新发布到指定的 DLX 上去，进而被路由到死信队列。因此可以监听该死信队列做相应的处理。

> PS：这个特性与将 TTL 设置为 0 配合使用可以实现 immediate 属性的效果。
>
> 回顾：immediate 属性是如果消息所在的队列没有消费者订阅，消息将被返回。
>
> 如何实现效果的？
> 1. 将 TTL 设置为 0：没有消费者订阅进入死信队列，
> 2. 监听死信队列，实现消息返回的逻辑。

**死信队列的实现方式是给一个正常的队列指定一个死信交换器（DLX）**，以下是实例代码（Producer）：

```java
public static void main(String[] args) throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setPort(5672);
    factory.setHost("127.0.0.1");
    factory.setUsername("root");
    factory.setPassword("root");

    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    /*---------------------------------------------------
      1. 创建一个死信交换器 DLX，实则跟正常交换器没有任何不同
      2. 创建一个死信队列，跟正常队列没有任何不同
      3. 死信交换器与死信队列绑定
     ---------------------------------------------------*/
    channel.exchangeDeclare("exchange.dlx", "direct", true, false, false, null);
    channel.queueDeclare("queue.dlx", true, false, false, null);
    channel.queueBind("queue.dlx", "exchange.dlx", "routingKey.dlx", null);

    // 设置正常队列的属性：1.设置队列消息过期时间，以便消息成为死信；2.设置该队列的死信交换器；3.设置该队列中消息到死信交换器的路由键（与上一行代码的绑定键保持一致）
    Map<String, Object> map = new HashMap<>();
    map.put("x-message-ttl", 10000);
    map.put("x-dead-letter-exchange", "exchange.dlx");
    map.put("x-dead-letter-routing-key", "routingKey.dlx");

    /*---------------------------------------------------
      1. 定义一个正常交换器
      2. 定义一个正常队列，通过添加队列参数指定「死信交换器 DLX」
      3. 正常交换器与正常队列绑定
     ---------------------------------------------------*/
    channel.exchangeDeclare("exchange.normal", "fanout", false, false, null);
    channel.queueDeclare("queue.normal", true, false, false, map);
    channel.queueBind("queue.normal", "exchange.normal", "routingKey.normal", null);

    String content = "这是一条测试死信队列（DLX）的消息，currentTime: " + LocalDateTime.now();
    channel.basicPublish("exchange.normal", "randomKey", true, false,
            new AMQP.BasicProperties().builder().deliveryMode(2).contentType("text/plain").build(),
            content.getBytes());

    connection.close();
}
```

代码含义在注释里写的很清楚了，这里需要注意的一点是不要把「死信队列」的写法和「备份交换器」的写法混淆了：

- 备份交换器是指定参数，添加到 **主交换器** 的定义方法上 `channel.eexchangeDeclare`。
- 死信队列是指定参数，添加到 **主队列** 的定义方法上 `channel.queueDeclare`。

**上述代码消息发送之后的执行流程：**

1. 生产者发送携带路由键为 randomKey 的消息，经过 exchange.normal 顺利存储到 queue.normal 中；
2. 由于 queue.normal 设置了过期时间，消息在 10s 内没有消费者消费该消息，判定消息过期，变成死信消息；
3. 由于 queue.normal 设置了 DLX，死信消息丢给了 exchange.dlx;
4. 根据 queue.normal 设置的 DLK（dead-letter-routingKey），exchange.dlx 匹配到了 queue.dlx，从而消息被存储到 queue.dlx 这个死信队列中去了。

![](http://img.yuzh.xyz/20190924231013_QVOi6a_Screenshot.png)

_验证 1：创建两个消费者，一个消费 queue.normal，一个消费 queue.dlx。单独启动一个消费者和生产者，再启动全部消费者和生产者，查看控制台输出_

_验证 2：不启动消费者，启动生产者。然后多次执行 rabbitmqctl list_queues 命令，查看输出效果。_

> DLX 是一个非常有用的特性，可以通过消费死信队列的消息来处理消息不能被正常消费的异常情况。从而改善和优化系统。
DLX 和 TTL 配合还可以实现「延迟队列」的功能。

## 延迟队列
## 优先级队列
## RPC 实现
## 持久化
## 生产者确认
### 事物机制
### 发送方确认机制
## 消费端要点介绍
### 消息分发
### 消息顺序性
### 启用 QueueingConsumer
## 消息传输保障
## 小结
