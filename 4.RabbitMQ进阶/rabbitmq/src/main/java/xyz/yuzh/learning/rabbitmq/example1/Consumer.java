package xyz.yuzh.learning.rabbitmq.example1;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 测试
 * @author Harry Zhang
 * @since 2019-09-24 14:35
 */
public class Consumer {

    public static void main(String[] args) throws IOException, TimeoutException {
        Address[] addresses = {
                new Address("127.0.0.1", 5672)
        };
        ConnectionFactory factory = new ConnectionFactory();
        Connection connection = factory.newConnection(addresses);
        Channel channel = connection.createChannel();

        channel.basicQos(64);
        channel.basicConsume(
                "demoQueue",
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
