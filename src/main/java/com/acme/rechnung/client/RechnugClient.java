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

public final class RechnugClient {
    private RechnugClient() {
    }

    public static void main(String[] args) throws Exception {
        // Verbindug zu grpc Service
        String grpcHost = umgebungswert("INVOICE_METADATA_HOST", "localhost");
        int grpcPort = Integer.parseInt(umgebungswert("INVOICE_METADATA_PORT", "50051"));

        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();

        // Ausführen der Anfrage
        // TODO Loop erstellen für mehr Anfragen + mehr Mock-Daten für die Anfragen
        try {
            while (true) {
                Rechnungsdaten gespeicherteMetadaten = saveRechnungMetadata(channel);
                veroeffentlicheZahlungsauftrag(gespeicherteMetadaten);
                System.out.printf(
                        "RechnugClient completed: invoiceId=%s supplier=%s amount=%s %s%n",
                        gespeicherteMetadaten.getRechnungsId(),
                        gespeicherteMetadaten.getLieferantenName(),
                        gespeicherteMetadaten.getGesamtbetragBrutto(),
                        gespeicherteMetadaten.getWaehrung()
                );
                Thread.sleep(500);
            }
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // Rechnung Metadaten erstellen
    private static Rechnungsdaten saveRechnungMetadata(ManagedChannel channel) {
        // Daten eingeben
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

    private static void veroeffentlicheZahlungsauftrag(Rechnungsdaten gespeicherteMetadaten) throws Exception {
        PaymentOrder paymentOrder = PaymentOrder.forInvoice(gespeicherteMetadaten);
        try (PaymentOrderPublisher publisher = new PaymentOrderPublisher()) {
            publisher.publish(paymentOrder);
        }
    }

    private static String umgebungswert(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
