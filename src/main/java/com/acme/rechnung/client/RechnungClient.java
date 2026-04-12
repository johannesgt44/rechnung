package com.acme.rechnung.client;

import com.acme.rechnung.invoice.v1.CreateRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.CreateRechnungMetadataResponse;
import com.acme.rechnung.invoice.v1.GetRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.GetRechnungMetadataResponse;
import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.invoice.v1.RechnungMetadataServiceGrpc;
import com.acme.rechnung.zahlung.Zahlungsauftrag;
import com.acme.rechnung.zahlung.ZahlungsauftragPublisher;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;

public final class RechnungClient {
    private RechnungClient() {
    }

    public static void main(String[] args) throws Exception {
        String grpcHost = umgebungswert("INVOICE_METADATA_HOST", "localhost");
        int grpcPort = Integer.parseInt(umgebungswert("INVOICE_METADATA_PORT", "50051"));

        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();

        try {
            Rechnungsdaten gespeicherteMetadaten = createRechnungMetadata(channel);
            Rechnungsdaten geleseneMetadaten = leseRechnungMetadata(channel, gespeicherteMetadaten.getRechnungsId());
            veroeffentlicheZahlungsauftrag(gespeicherteMetadaten);
            System.out.printf(
                    "RechnungClient completed: invoiceId=%s supplier=%s amount=%s %s%n",
                    geleseneMetadaten.getRechnungsId(),
                    geleseneMetadaten.getLieferantenName(),
                    geleseneMetadaten.getGesamtbetragBrutto(),
                    geleseneMetadaten.getWaehrung()
            );
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static Rechnungsdaten createRechnungMetadata(ManagedChannel channel) {
        Rechnungsdaten metadata = Rechnungsdaten.newBuilder()
                .setLieferantenName("Muster Lieferant GmbH")
                .setRechnungsNummer("RE-2026-0001")
                .setRechnungsDatum("2026-04-07")
                .setGesamtbetragBrutto("1190.00")
                .setWaehrung("EUR")
                .build();

        CreateRechnungMetadataRequest request = CreateRechnungMetadataRequest.newBuilder()
                .setMetadata(metadata)
                .build();

        RechnungMetadataServiceGrpc.RechnungMetadataServiceBlockingStub stub =
                RechnungMetadataServiceGrpc.newBlockingStub(channel);
        CreateRechnungMetadataResponse response = stub.createRechnungMetadata(request);
        return response.getMetadata();
    }

    private static Rechnungsdaten leseRechnungMetadata(ManagedChannel channel, String rechnungsId) {
        GetRechnungMetadataRequest request = GetRechnungMetadataRequest.newBuilder()
                .setRechnungsId(rechnungsId)
                .build();

        RechnungMetadataServiceGrpc.RechnungMetadataServiceBlockingStub stub =
                RechnungMetadataServiceGrpc.newBlockingStub(channel);
        GetRechnungMetadataResponse response = stub.getRechnungMetadata(request);
        return response.getMetadata();
    }

    private static void veroeffentlicheZahlungsauftrag(Rechnungsdaten gespeicherteMetadaten) throws Exception {
        Zahlungsauftrag zahlungsauftrag = Zahlungsauftrag.toZahlungsauftrag(gespeicherteMetadaten);
        try (ZahlungsauftragPublisher publisher = new ZahlungsauftragPublisher()) {
            publisher.publish(zahlungsauftrag);
        }
    }

    private static String umgebungswert(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
