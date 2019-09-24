package xyz.yuzh.learning.rabbitmq;

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
 * @author Harry Zhang
 * @since 2019-09-21 15:32
 */
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
