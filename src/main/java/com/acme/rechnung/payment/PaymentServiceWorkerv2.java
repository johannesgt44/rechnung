package com.acme.rechnung.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

/**
 * Klasse zum Starten eines Workers für den Zahlungsservice
 * Verbindungsaufbau mit RabbitMQ
 */
public class PaymentServiceWorkerv2 {

    static Connection connection;
    static Channel channel;
    static ObjectMapper objectMapper;
    static String queueName;

    private PaymentServiceWorkerv2() {

    }

    /**
     * Einstiegpunkt des Programms, startet also den Zahlungsservice
     * Try- und Catch-Teil, weil damit angegeben wird, dass Exceptions
     * auftreten können, da versucht wird Verbindungen zu Servern(RabbitMQ) aufzubauen
     * welche z.B. nicht gestartet sein können. Diese Exceptions können nicht an eine aufrufende
     * Methode weitergeleitet werden, da es sich hier um die Start-Methode handelt.
     */
    public static void main(final String[] args) {
        try{
            /*
             * ObjektMapper um aus JSON-Daten Java-Objekte zu parsen und umgekehrt
             * Grund ist, dass RabbitMQ die Daten in einem JSON Format versendet
             */
            objectMapper = new ObjectMapper();
            /* Name der RabbitMQ Queue*/
            queueName = PaymentQueueConfig.queueName();
            /*
             * Durch den Methoden Aufruf der Konfigurationsklasse werden die erforderlichen Daten geholt
             * ein Java Objekt daraus gebildet und eine echte Verbindung zu RabbitMQ aufgebaut
             */
            connection = PaymentQueueConfig.connectionFactory().newConnection();

            /* Erzeugung eines Verbindungskanal zu RabbitMQ */
            channel = connection.createChannel();

            /* Anlegen und konfigurieren der Queue bei RabbitMQ (falls nicht geschehen) */
            channel.queueDeclare(queueName, true, false, false, null);

            /* Definition, dass der Worker maximal eine unbearbeitete Nachricht gleichzeitig bekommt */
            channel.basicQos(1);

        }
        catch(Exception ex){
            System.err.println("Beim starten des Zahlungsservices ist folgender Fehler aufgetreten: "
                    + ex.getMessage());
        }

        /*
         * Shutdown-Hook, sodass beim Beenden des Programms offnene Ressourcen sauber
         * geschlossen werden. Also die Verbindung zu RabbitMQ beendet werden.
         * */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                channel.close();
                connection.close();
            }
            catch(Exception _){
                System.err.println("Fehler beim Beenden der Verbindung zu RabbitMQ.");
            }
        }));

        /*
         * Definition was bei einer eingehenden Nachricht passieren soll.
         * DeliverCallback entspricht der empfangenen Nachricht von RabbitMQ.
         */
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try{
                /* Nimmt die Anfrage, speichert diese, und liest den Body der Nachricht aus. */
                PaymentOrder order = objectMapper.readValue(delivery.getBody(), PaymentOrder.class);
                System.out.println("Zahlung verarbeitet: paymentId="+order.paymentId()+" invoiceId="+order.invoiceId()
                        +" amount="+order.amount()+" "+order.currency());
                /* Worker bestätigt RabbitMQ, dass die Nachricht erfolgreich verarbeitet wurde.*/
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
            catch(Exception ex){
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                System.err.println("Zahlung konnte nicht verarbeitet werden:" + ex.getMessage());
            }
        };
        /* Startet den Worker für die angegebene Queue(queueName) und verarbeitet eingehende Nachrichten.*/
        try {
            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
            System.out.println("Worker wartet auf eine eingehende Queue '"+queueName+"'");
        }
        catch (Exception _) {
            System.out.println("Fehler beim starten des Workers.");
        }
    }
}
