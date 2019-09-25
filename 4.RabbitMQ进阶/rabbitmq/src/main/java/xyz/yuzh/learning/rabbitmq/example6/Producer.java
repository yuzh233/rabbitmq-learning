package xyz.yuzh.learning.rabbitmq.example6;

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
 * 优先级队列
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

        Map<String, Object> map = new HashMap<>();
        // 设置队列的最高级别是多少
        map.put("x-max-priority", 10);

        channel.exchangeDeclare("exchange.normal", "fanout", true, false, false, null);
        channel.queueDeclare("queue.normal", true, false, false, map);
        channel.queueBind("queue.normal", "exchange.normal", "routingKey.normal", null);

        String content = "这是一条具有优先级的消息，currentTime: " + LocalDateTime.now();
        channel.basicPublish("exchange.normal", "random.routingKey", true, false,
                new AMQP.BasicProperties().builder().deliveryMode(2).contentType("text/plain")
                        // 设置该条消息的级别是 5
                        .priority(5)
                        .build(),
                content.getBytes());

        connection.close();
    }
}
