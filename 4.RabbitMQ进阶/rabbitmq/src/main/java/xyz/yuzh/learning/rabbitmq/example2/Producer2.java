package xyz.yuzh.learning.rabbitmq.example2;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 测试备份交换器
 *
 * @author Harry Zhang
 * @since 2019-09-24 14:35
 */
public class Producer2 {

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("root");
        factory.setPassword("root");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // 创建备份交换器和备份队列，使其绑定。注意：类型为fanout，即 "把消息路由到所有与之绑定的队列上去，忽略路由键。"
        channel.exchangeDeclare("alternateExchange", "fanout", true, false, false, null);
        channel.queueDeclare("alternateQueue", true, false, false, null);
        channel.queueBind("alternateQueue", "alternateExchange", "alternateRoutingKey", null);

        // 路由键参数, alternate-exchange 是固定参数
        Map<String, Object> map = new HashMap<>();
        map.put("alternate-exchange", "alternateExchange");

        // 创建正常交换器和正常队列，使其绑定。并将 alternateExchange 作为该交换器的备份交换器。
        channel.exchangeDeclare("normalExchange", "direct", true, false, false, map);
        channel.queueDeclare("normalQueue", true, false, false, null);
        channel.queueBind("normalQueue", "normalExchange", "normalRoutingKey", null);

        String content = "收到一条消息：" + LocalDateTime.now();
        channel.basicPublish(
                "normalExchange",
                "normalRoutingKey111",
                // mandatory 参数已经无效了，设置了 AE 消息就不会返回了。
                true,
                false,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                content.getBytes());

        channel.addReturnListener(new ReturnListener() {
            @Override
            public void handleReturn(int replyCode, String replyText, String exchange, String routingKey,
                                     AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("消息返回生产者，内容是：" + new String(body));
            }
        });

        TimeUnit.SECONDS.sleep(1);
        connection.close();
    }

}
