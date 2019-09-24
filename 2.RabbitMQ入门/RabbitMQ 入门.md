---
title: RabbitMQ 入门
date: 2019-09-21 10:00
toc: true
tag: rabbit-mq
thumbnail: http://img.yuzh.xyz/20190921153530_iYzWDR_ksenia-makagonova-LuK-MuZ-yf0-unsplash.jpeg
---

<h1>第二章：RabbitMQ 入门</h1>

<h2>Table Of Contents</h2>

<!-- TOC -->

- [生产者（Producer）](#%E7%94%9F%E4%BA%A7%E8%80%85producer)
- [消费者（Consumer）](#%E6%B6%88%E8%B4%B9%E8%80%85consumer)
- [服务节点（Broker）](#%E6%9C%8D%E5%8A%A1%E8%8A%82%E7%82%B9broker)
- [队列（Queue）](#%E9%98%9F%E5%88%97queue)
- [交换器（Exchange）](#%E4%BA%A4%E6%8D%A2%E5%99%A8exchange)
- [路由键（RoutingKey）](#%E8%B7%AF%E7%94%B1%E9%94%AEroutingkey)
- [绑定与绑定键（Bind&BindingKey）](#%E7%BB%91%E5%AE%9A%E4%B8%8E%E7%BB%91%E5%AE%9A%E9%94%AEbindbindingkey)
- [路由键与绑定键的区别](#%E8%B7%AF%E7%94%B1%E9%94%AE%E4%B8%8E%E7%BB%91%E5%AE%9A%E9%94%AE%E7%9A%84%E5%8C%BA%E5%88%AB)
- [交换器类型](#%E4%BA%A4%E6%8D%A2%E5%99%A8%E7%B1%BB%E5%9E%8B)
- [RabbitMQ 运转流程](#rabbitmq-%E8%BF%90%E8%BD%AC%E6%B5%81%E7%A8%8B)
- [Connection 和 Channel](#connection-%E5%92%8C-channel)
- [AMQP 协议](#amqp-%E5%8D%8F%E8%AE%AE)

<!-- /TOC -->

RabbitMQ 整体上是一个生产者与消费者模型，主要负责接收、存储、转发消息。RabbitMQ 的整体模型架构如下所示：

![](http://img.yuzh.xyz/20190921104045_rbAdMv_Screenshot.png)

<!-- more -->
## 生产者（Producer）
投递消息的一方，生产者创建消息，然后发布到 RabbitMQ 中。消息一般分为两个部分：消息体和标签。

消息体是带业务逻辑的数据，比如一个 JSON 字符串，或者是一个序列化后的对象。

标签在 RabbitMQ 中的具体体现是`交换机`和`路由键`，生产者通过指定标签，MQ 可以根据标签把消息发送给感兴趣的消费者。

## 消费者（Consumer）
接收消息的一方，消费者连接到 RabbitMQ 服务器，订阅到队列上。当消费一条消息时，只是消费消息的消息体。在消息路由的过程中，标签会被丢弃，存入队列中的只有消息体，消费者不知道生产者是谁。

## 服务节点（Broker）

一个 RabbitMQ Broker 可以简单看作成一个 RabbitMQ 的服务节点或者 RabbitMQ 服务示例。大多数情况下也可以将 RabbitMQ Broker 看作成一个 RabbitMQ 服务器。

下面展示了生产者将消息存入 RabbitMQ Broker，以及消费者从 Broker 中消费数据的整体流程：

![](http://img.yuzh.xyz/20190921105657_wKqWAP_Screenshot.png)

## 队列（Queue）
队列是 RabbitMQ 中用于存储消息的内部对象，生产者生产的消息最终会投递到队列当中去，消费者会从队列中获取消息并消费。

多个消费者可以订阅同一个队列，此时队列中的消息会被平均分摊（即：轮询）给多个消费者进行处理，而不是每个消费者都收到所有的消息并处理。RabbitMQ 不支持队列层面的广播消费。

![](http://img.yuzh.xyz/20190921110154_dhV7BR_Screenshot.png)

## 交换器（Exchange）
交换器可以看作是消息从发送到最终存储队列的路由器。生产者将消息发送到交换器，交换器将消息转发到一个或多个队列中。如果路由不到也许或返回给生产者，也许会直接丢弃。

![](http://img.yuzh.xyz/20190921110736_pWnEvi_Screenshot.png)

## 路由键（RoutingKey）
生产者将消息发送给交换器时，通过指定 RoutingKey，用来指定这个消息的路由规则，即消息通过 RoutingKey 的规则决定最终存储到哪个队列。

> 注意：该路由键需要与交换器类型和绑定键联合使用才能生效。（该概念后续会介绍）

## 绑定与绑定键（Bind&BindingKey）
在发送消息之前，需要先将交换器与队列通过 BandingKey 绑定起来，这样 RabbitMQ 就知道后续如何将消息正确的路由给队列了。

![](http://img.yuzh.xyz/20190921111629_iJSK2d_Screenshot.png)

在绑定多个队列到同一个交换器时，允许指定相同的 Bingkey。

## 路由键与绑定键的区别
在发送消息之前，通过 BindingKey 将 Exchange 和 Queue 绑定起来。然后发送消息时将指定 RoutingKey，当 RoutingKey 与 BindingKey 相匹配时，就会路由到绑定的那个队列上去了。

如上图所示，发送消息时：

- 若 RoutingKey = "Binding Key 1"，将会路由到 Queue1
- 若 RoutingKey = "Binding Key 2"，将会路由到 Queue2
- 若 RoutingKey = "Binding Key 3"，消息将会返回给生产者或被丢弃

## 交换器类型
RabbitMQ 常用的交换器类型有：fanout、direct、topic、headers 四种。

**fanout**

它会把所有发送到该交换器的消息路由到所有与该交换器绑定的队列上去。

**direct**

将消息路由到 RoutingKey 与 BingKey 完全匹配的队列中。

**topic**

topic 在 direct 之上做了拓展，它也是将消息路由到 RoutingKey 与 BingKey 匹配的队列中，不过可以允许模糊匹配：

- RoutingKey 和 BindingKey 规则都是以 "." 号分割的字符串；
- BindingKey 可以存在两种特殊字符 `"*"`、`"#"`。`"*"`用于匹配一个单词，`"#"`用于匹配多个单词。

如：
![](http://img.yuzh.xyz/20190921113552_nGoaEe_Screenshot.png)

- `RoutingKey = "com.rabbitmq.client"` 的消息会同时路由到 Queue1 和 Queue2
- `RoutingKey = "com.hidden.client"` 的消息只会路由到 Queue2
- `RoutingKey = "com.hidden.demo"` 的消息只会路由到 Queue2
- `RoutingKey = "java.rabbitmq.demo"` 的消息只会路由到 Queue1
- `RoutingKey = "java.util.concurrent"` 没有匹配任何路由键将会被返回生产者或丢弃

**headers**

该类型的交换器不依赖与路由键的匹配规则来路由消息，而是根据发送的消息内容的 headers 属性来进行匹配。在绑定队列和交换器时指定一组键值对，当发送消息到交换器时，MQ 会获取到该消息的 headers，对比其中的键值对是否完全匹配队列与交换器绑定时的键值对，如果完全匹配则路由消息到该队列。

headers 类型的交换器性能很差，不实用，使用很少。

## RabbitMQ 运转流程

**生产者发送消息的过程：**

1. 生产者连接到 RabbitMq Broker，建立一个连接（Connection），开启一个信道（Channel）；
2. 生产者申明一个交换器，设置相关属性，如交换器类型、是否持久化等；
3. 生产者申明一个队列并设置相关属性如：是否排他、是否持久化、是否自动删除等；
4. 生产者通过绑定键将交换器与队列绑定起来；
5. 生产者发送消息到 RabbitMQ Broker，其中包括路由键、交换器等信息；
6. 交换器根据收到的路由键查找相对应的队列；
7. 如果找到，将从生产者发送过来的消息存入到相应的队列中；
8. 如果没有找到，根据生产者配置的属性选择丢弃还是返回消息给生产者；
9. 关闭信道；
10. 关闭连接。

**消费者消费消息的过程：**

1. 消费者连接到 RabbitMQ Broker，建立一个 Connection，开启一个 Channel；
2. 消费者向 RabbitMQ Broker 请求消费相应队列中的消息，可能会设置相应的回调函数以及做一些准备工作；
3. 等待 RabbitMQ Broker 回应并投递相应队列中的消息，消费者接收消息；
4. 消费者确认（ack）接收到消息；
5. RabbitMQ 从队列中删除相应以及被确认的消息；
6. 关闭信道；
7. 关闭连接。

## Connection 和 Channel
生产者和消费者都需要和 RabbitMQ Broker 建立连接，这个连接是一条 TCP 连接，也就是 Connection。之后客户端可以在 Connection 上建立 Channel，Channel 是建立在 Connection 之上的虚拟连接，RabbitMQ 处理每一条指令都是通过 Channel 完成的。

![](http://img.yuzh.xyz/20190921141700_o7TXSO_Screenshot.png)

使用 Channel 的目的是实现 Connection 的多路复用，如果每一个消息的生产和消费都创建一个 Connection，那就会产生很多个 TCP 连接。操作系统创建 TCP 连接是非常昂贵的开销，遇到使用高峰期，性能瓶颈很快就会凸显出来。

## AMQP 协议
AMQP 是一个应用层的通信协议，其填充于 TCP 协议的数据部分。AMQP 协议可以看作是一系列结构化命令的集合，其通过协议命令进行交互。

RabbitMQ 是遵循 AMQP 协议的 Erlang 实现，AMQP 的模型架构和 RabbitMQ 的模型架构是一样的，RabbitMQ 中的交换器、交换器类型、队列、绑定、路由键都是遵循 AMQP 协议中相应的概念来的。
