package xyz.yuzh.learning.rabbitmq.example4;

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
 * @since 2019-09-24 22:25
 */
public class Producer {
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
}
