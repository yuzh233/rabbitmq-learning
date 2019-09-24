package xyz.yuzh.learning.rabbitmq.example1;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 测试 mandatory 参数和 immediate 参数
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
        channel.queueDeclare("demoQueue", true, false, false, null);
        channel.queueBind("demoQueue", "demoExchange", "demoRoutingKey", null);

        String content = "收到一条消息：" + LocalDateTime.now();
        channel.basicPublish(
                "demoExchange",
                // 模拟消息发送失败，将路由键 demoRoutingKey 置为空，消息不可达。
                "",
                // mandatory 参数为 true，消息不可达时将返回给生产者
                true,
                // immediate 参数不建议使用，一般为 false 即可。
                false,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                content.getBytes());

        // 消息 return 监听器
        channel.addReturnListener(new ReturnListener() {
            @Override
            public void handleReturn(int replyCode, String replyText, String exchange, String routingKey,
                                     AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("消息返回生产者，内容是：" + new String(body));
            }
        });

        // 为什么要睡一秒再关闭？RabbitMQ 客户端中的回调函数和 channel 不是同一个线程，有可能出现 channel 进程关闭了而回调函数没有执行到的情况。
        TimeUnit.SECONDS.sleep(1);
        connection.close();
    }

}
