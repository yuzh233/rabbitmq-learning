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
## 死信队列（DLX）
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
