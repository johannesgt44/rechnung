package com.acme.rechnung.client;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.invoice.v1.RechnungMetadataServiceGrpc;
import com.acme.rechnung.invoice.v1.SaveRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.SaveRechnungMetadataResponse;
import com.acme.rechnung.payment.PaymentOrder;
import com.acme.rechnung.payment.PaymentOrderPublisher;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;

public final class InvoiceClient {
    private InvoiceClient() {
    }

    public static void main(String[] args) throws Exception {
        String grpcHost = environmentValue("INVOICE_METADATA_HOST", "localhost");
        int grpcPort = Integer.parseInt(environmentValue("INVOICE_METADATA_PORT", "50051"));

        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();

        try {
            Rechnungsdaten gespeicherteMetadaten = saveRechnungMetadata(channel);
            publishPaymentOrder(gespeicherteMetadaten);
            System.out.printf(
                    "InvoiceClient completed: invoiceId=%s supplier=%s amount=%s %s%n",
                    gespeicherteMetadaten.getRechnungsId(),
                    gespeicherteMetadaten.getLieferantenName(),
                    gespeicherteMetadaten.getGesamtbetragBrutto(),
                    gespeicherteMetadaten.getWaehrung()
            );
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static Rechnungsdaten saveRechnungMetadata(ManagedChannel channel) {
        Rechnungsdaten metadata = Rechnungsdaten.newBuilder()
                .setLieferantenName("Muster Lieferant GmbH")
                .setRechnungsNummer("RE-2026-0001")
                .setRechnungsDatum("2026-04-07")
                .setGesamtbetragBrutto("1190.00")
                .setWaehrung("EUR")
                .build();

        SaveRechnungMetadataRequest request = SaveRechnungMetadataRequest.newBuilder()
                .setMetadata(metadata)
                .build();

        RechnungMetadataServiceGrpc.RechnungMetadataServiceBlockingStub stub =
                RechnungMetadataServiceGrpc.newBlockingStub(channel);
        SaveRechnungMetadataResponse response = stub.saveRechnungMetadata(request);
        return response.getMetadata();
    }

    private static void publishPaymentOrder(Rechnungsdaten gespeicherteMetadaten) throws Exception {
        PaymentOrder paymentOrder = PaymentOrder.forInvoice(gespeicherteMetadaten);
        try (PaymentOrderPublisher publisher = new PaymentOrderPublisher()) {
            publisher.publish(paymentOrder);
        }
    }

    private static String environmentValue(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
