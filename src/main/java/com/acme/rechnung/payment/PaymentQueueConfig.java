package com.acme.rechnung.payment;

import com.rabbitmq.client.ConnectionFactory;

final class PaymentQueueConfig {
    static final String DEFAULT_QUEUE_NAME = "payment.orders";

    private PaymentQueueConfig() {
    }

    static String queueName() {
        return environmentValue("PAYMENT_QUEUE", DEFAULT_QUEUE_NAME);
    }

    static ConnectionFactory connectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(environmentValue("RABBITMQ_HOST", "localhost"));
        factory.setPort(Integer.parseInt(environmentValue("RABBITMQ_PORT", "5672")));
        factory.setUsername(environmentValue("RABBITMQ_USERNAME", "guest"));
        factory.setPassword(environmentValue("RABBITMQ_PASSWORD", "guest"));
        return factory;
    }

    private static String environmentValue(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
