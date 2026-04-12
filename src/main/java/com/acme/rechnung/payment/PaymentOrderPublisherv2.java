package com.acme.rechnung.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public final class PaymentOrderPublisherv2 implements AutoCloseable {
    //XML Mapper
    private final ObjectMapper objectMapper = new ObjectMapper();
    // Connection und Channel für RabbitMQ
    private final Connection connection;
    private final Channel channel;
    private final String queueName;

    public PaymentOrderPublisherv2() throws IOException, TimeoutException {
        // Attribute setzten, Werte in QueueConfig
        this.queueName = PaymentQueueConfig.queueName();
        this.connection = PaymentQueueConfig.connectionFactory().newConnection();

        // Channel wird erstellt
        this.channel = connection.createChannel();

        // In dem Channel wird die Queue angelegt
        this.channel.queueDeclare(queueName, true, false, false, null);
    }

    // PaymentOrder in die Queue schreiben
    // TODO was macht AQMP
    public void publish(PaymentOrderv2 paymentOrder) throws IOException {
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
