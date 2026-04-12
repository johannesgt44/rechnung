package com.acme.rechnung.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

public final class PaymentServiceWorker {
    private PaymentServiceWorker() {
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String queueName = PaymentQueueConfig.queueName();

        Connection connection = PaymentQueueConfig.connectionFactory().newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, true, false, false, null);
        channel.basicQos(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                channel.close();
                connection.close();
            } catch (Exception ignored) {
                // Shutdown hook must not prevent the JVM from exiting.
            }
        }));

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                Zahlungsauftrag order = objectMapper.readValue(delivery.getBody(), Zahlungsauftrag.class);
                System.out.printf(
                        "Payment processed: paymentId=%s invoiceId=%s amount=%s %s%n",
                        order.paymentId(),
                        order.invoiceId(),
                        order.amount(),
                        order.currency()
                );
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception exception) {
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                System.err.printf("Payment order rejected: %s%n", exception.getMessage());
            }
        };

        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
        System.out.printf("PaymentServiceWorker waits for messages on queue '%s'%n", queueName);
    }
}
