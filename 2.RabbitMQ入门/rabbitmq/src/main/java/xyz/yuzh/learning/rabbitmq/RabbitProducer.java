package xyz.yuzh.learning.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

/**
 * @author Harry Zhang
 * @since 2019-09-18 17:00
 */
public class RabbitProducer {

    private static final String EXCHANGE_NAME = "exchange_demo";
    private static final String ROUTING_KEY = "routingkey_demo";
    private static final String QUEUE_NAME = "queue_demo";
    private static final String IP_ADDRESS = "127.0.0.1";

    /**
     * RabbitMQ 服务端默认端口号
     */
    private static final int PORT = 5672;


    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(IP_ADDRESS);
        factory.setPort(PORT);
        factory.setUsername("root");
        factory.setPassword("root");

        // 创建连接，获取信道。
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // 创建一个 type = "direct"、持久化、非自动删除的「交换器」
        channel.exchangeDeclare(EXCHANGE_NAME, "direct", true, false, null);

        // 创建一个持久化、非排他、非自动删除的「队列」
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);

        // 将交换机与队列通过「路由键」绑定
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);

        // 发送一条持久化消息
        String message = "Hello RabbitMQ! now: " + LocalDateTime.now();
        channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                message.getBytes());

        // 关闭资源
        channel.close();
        connection.close();
    }

}
