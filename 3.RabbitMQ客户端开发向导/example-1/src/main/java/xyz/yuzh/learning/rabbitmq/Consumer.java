package xyz.yuzh.learning.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author Harry Zhang
 * @since 2019-09-21 15:32
 */
public class Consumer {

    private static final String QUEUE = "queueDemo";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setVirtualHost("/");
        factory.setUsername("root");
        factory.setPassword("root");
        Address[] addresses = {
                new Address("127.0.0.1", 5672)
        };
        Connection connection = factory.newConnection(addresses);
        Channel channel = connection.createChannel();
        channel.basicQos(64);
        /*
          持续订阅消息
         */
        channel.basicConsume(QUEUE, false, "consumerTag", true, false, null,
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                               byte[] body) throws IOException {
                        String routingKey = envelope.getRoutingKey();
                        String contentType = properties.getContentType();
                        long deliveryTag = envelope.getDeliveryTag();
                        // ...... 业务逻辑
                        System.out.println(new String(body));
                        channel.basicAck(deliveryTag, false);
                    }
                });

        /*
          获得单条消息
         */
        // GetResponse response = channel.basicGet(QUEUE, false);
        // System.out.println(new String(response.getBody()));
        // channel.basicAck(response.getEnvelope().getDeliveryTag(), false);

//        channel.close();
//        connection.close();
    }

}
