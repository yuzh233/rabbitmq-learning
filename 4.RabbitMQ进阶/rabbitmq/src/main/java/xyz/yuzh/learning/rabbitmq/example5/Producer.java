package xyz.yuzh.learning.rabbitmq.example5;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 利用死信队列实现延迟队列的效果
 *
 * @author Harry Zhang
 * @since 2019-09-25 10:41
 */
public class Producer {
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setUsername("root");
        factory.setPassword("root");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // 设置死信队列
        channel.exchangeDeclare("exchange.dlx", "direct", true, false, false, null);
        channel.queueDeclare("queue.dlx", true, false, false, null);
        channel.queueBind("queue.dlx", "exchange.dlx", "routingKey.dlx", null);

        // 设置正常队列
        Map<String, Object> map = new HashMap<>();
        map.put("x-message-ttl", 6000);  // 正常队列的消息过期时间 6s
        map.put("x-dead-letter-exchange", "exchange.dlx");  // 正常队列绑定的死信交换器
        map.put("x-dead-letter-routing-key", "routingKey.dlx"); // 消息从正常队列到死信交换器的路由键

        channel.exchangeDeclare("exchange.normal", "fanout", true, false, false, null);
        channel.queueDeclare("queue.normal", true, false, false, map);
        channel.queueBind("queue.normal", "exchange.normal", "routingKey.normal", null);

        String content = "这是来自延迟队列的消息，currentTime: " + LocalDateTime.now();
        channel.basicPublish("exchange.normal", "random.routingKey", true, false,
                new AMQP.BasicProperties().builder().deliveryMode(2).contentType("text/plain").build(),
                content.getBytes());

        connection.close();
    }
}
