package xyz.yuzh.learning.rabbitmq.example3;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 测试 TTL
 *
 * @author Harry Zhang
 * @since 2019-09-24 14:35
 */
public class Producer {

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("root");
        factory.setPassword("root");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare("demoExchange", "direct", true, false, false, null);

        Map<String, Object> map = new HashMap<>();
        // 设置整个队列中消息的过期时间
        map.put("x-message-ttl", 6000);
        // 设置队列的过期时间
        map.put("x-expires",1800000);
        // channel.queueDeclare("demoQueue", true, false, false, map);
        channel.queueDeclare("demoQueue", true, false, false, null);
        channel.queueBind("demoQueue", "demoExchange", "demoRoutingKey", null);

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

        TimeUnit.SECONDS.sleep(1);
        connection.close();
    }

}
