/*Selbstgecoded mit KI Vorschlag als Template(Unterer Part)*/

package com.acme.rechnung.zahlung;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public final class ZahlungsauftragPublisher implements AutoCloseable {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Connection connection;
    private final Channel channel;
    private final String queueName;

    public ZahlungsauftragPublisher() throws IOException, TimeoutException {
        this.queueName = ZahlungsQueueConfig.queueName();
        this.connection = ZahlungsQueueConfig.connectionFactory().newConnection();
        this.channel = connection.createChannel();
        this.channel.queueDeclare(queueName, true, false, false, null);
    }

    public void publish(Zahlungsauftrag zahlungsauftrag) throws IOException {
        byte[] body = objectMapper.writeValueAsString(zahlungsauftrag).getBytes(StandardCharsets.UTF_8);
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
