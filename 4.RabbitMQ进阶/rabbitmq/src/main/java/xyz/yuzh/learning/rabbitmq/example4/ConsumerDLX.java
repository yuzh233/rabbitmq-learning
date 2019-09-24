package xyz.yuzh.learning.rabbitmq.example4;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 测试：消费死信队列的消息
 *
 * @author Harry Zhang
 * @since 2019-09-24 22:25
 */
public class ConsumerDLX {
    public static void main(String[] args) throws IOException, TimeoutException {
        Address[] addresses = {
                new Address("127.0.0.1", 5672)
        };
        ConnectionFactory factory = new ConnectionFactory();
        Connection connection = factory.newConnection(addresses);
        Channel channel = connection.createChannel();

        channel.basicQos(64);
        channel.basicConsume(
                "queue.dlx",
                false,
                "demoConsumerTag",
                false,
                false,
                null,
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                               byte[] body) throws IOException {
                        System.out.println(new String(body));
                        // 手动确认消息
                        channel.basicAck(envelope.getDeliveryTag(), false);
                    }
                });

        // connection.close();
    }
}
