package com.acme.rechnung.client;

import com.acme.rechnung.invoice.v1.InvoiceMetadata;
import com.acme.rechnung.invoice.v1.InvoiceMetadataServiceGrpc;
import com.acme.rechnung.invoice.v1.SaveInvoiceMetadataRequest;
import com.acme.rechnung.invoice.v1.SaveInvoiceMetadataResponse;
import com.acme.rechnung.payment.PaymentOrder;
import com.acme.rechnung.payment.PaymentOrderPublisher;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;

public final class InvoiceClient {
    private InvoiceClient() {
    }

    static void main() throws Exception {
        // Verbindug zu grpc Service
        String grpcHost = environmentValue("INVOICE_METADATA_HOST", "localhost");
        int grpcPort = Integer.parseInt(environmentValue("INVOICE_METADATA_PORT", "50051"));

        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();

        // Ausführen der Anfrage
        // TODO Loop erstellen für mehr Anfragen + mehr Mock-Daten für die Anfragen
        try {
            while (true) {
                InvoiceMetadata storedMetadata = saveInvoiceMetadata(channel);
                publishPaymentOrder(storedMetadata);
                System.out.printf(
                        "InvoiceClient completed: invoiceId=%s supplier=%s amount=%s %s%n",
                        storedMetadata.getInvoiceId(),
                        storedMetadata.getSupplierName(),
                        storedMetadata.getGrossAmount(),
                        storedMetadata.getCurrency()
                );
                Thread.sleep(500);
            }
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // Rechnung Metadaten erstellen
    private static InvoiceMetadata saveInvoiceMetadata(ManagedChannel channel) {
        // Daten eingeben
        InvoiceMetadata metadata = InvoiceMetadata.newBuilder()
                .setSupplierName("Muster Lieferant GmbH")
                .setInvoiceNumber("RE-2026-0001")
                .setInvoiceDate("2026-04-07")
                .setGrossAmount("1190.00")
                .setCurrency("EUR")
                .build();

        SaveInvoiceMetadataRequest request = SaveInvoiceMetadataRequest.newBuilder()
                .setMetadata(metadata)
                .build();

        InvoiceMetadataServiceGrpc.InvoiceMetadataServiceBlockingStub stub =
                InvoiceMetadataServiceGrpc.newBlockingStub(channel);
        SaveInvoiceMetadataResponse response = stub.saveInvoiceMetadata(request);
        return response.getMetadata();
    }

    //
    private static void publishPaymentOrder(InvoiceMetadata storedMetadata) throws Exception {
        PaymentOrder paymentOrder = PaymentOrder.forInvoice(storedMetadata);
        try (PaymentOrderPublisher publisher = new PaymentOrderPublisher()) {
            publisher.publish(paymentOrder);
        }
    }

    //
    private static String environmentValue(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
