package com.acme.rechnung.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public final class PaymentOrderPublisher implements AutoCloseable {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Connection connection;
    private final Channel channel;
    private final String queueName;

    public PaymentOrderPublisher() throws IOException, TimeoutException {
        this.queueName = PaymentQueueConfig.queueName();
        this.connection = PaymentQueueConfig.connectionFactory().newConnection();
        this.channel = connection.createChannel();
        this.channel.queueDeclare(queueName, true, false, false, null);
    }

    public void publish(PaymentOrder paymentOrder) throws IOException {
        byte[] body = objectMapper.writeValueAsString(paymentOrder).getBytes(StandardCharsets.UTF_8);
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(2)
                .build();

        channel.basicPublish("", queueName, properties, body);
    }

    @Override
    public void close() throws Exception {
        channel.close();
        connection.close();
    }
}
