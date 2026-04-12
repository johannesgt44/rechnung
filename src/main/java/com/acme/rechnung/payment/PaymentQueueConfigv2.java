package com.acme.rechnung.payment;

import com.rabbitmq.client.ConnectionFactory;
/**
 * Klasse zur Konfiguration der Queues des Zahlungsservices für eine asynchrone
 * Kommunikation mit dem Service.
 */
final class PaymentQueueConfigv2 {
    /** Default Name/Wert der eingehenden Queues.*/
    static final String DEFAULT_NAME = "payment.orders";

    /** Leerer Kontruktor */
    PaymentQueueConfigv2(){

    }

    /** Gibt den Namen der aktuelle Queue zurück.*/
    static String queueName(){
        return environmentValue("PAYMENT_QUEUE", DEFAULT_NAME);
    }

    /** Erstellt und konfiguriert eine ConnectionFactory für die Verbindung zu RabbitMQ.*/
    static ConnectionFactory connectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(environmentValue("RABBITMQ_HOST", "localhost"));
        factory.setPort(Integer.parseInt(environmentValue("RABBITMQ_PORT", "5672")));
        factory.setUsername(environmentValue("RABBITMQ_USERNAME", "guest"));
        factory.setPassword(environmentValue("RABBITMQ_PASSWORD", "guest"));
        return factory;
    }

    private static String environmentValue(String key, String defaultValue) {
        /*
         * Holt aus der Umgebungsvariable den Namen der Queue.
         * Muss jedoch nicht angegeben sein, deshalb wurde ein default Name definiert.
         */
        String wert = System.getenv(key);
        /* Wenn kein Name angegeben wurde soll der default Wert verwendet werden.*/
        return wert == null || wert.isBlank() ? DEFAULT_NAME : wert;
    }

}
