/*Selbstgecoded mit KI als Template, überarbeitet mit KI, anschließend wieder eigene Ergänzung*/
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
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class RechnungClient {
    private RechnungClient() {
    }

    static void main() throws Exception {
        // Verbindung zu grpc-Service
        String grpcHost = umgebungswert("INVOICE_METADATA_HOST", "localhost");
        int grpcPort = Integer.parseInt(umgebungswert("INVOICE_METADATA_PORT", "50051"));

        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();

        try {
            // Schleife damit es für alle Mock-Metadaten durchläuft
            for (Rechnungsdaten testdaten : createMockRechnungen()) {
                Rechnungsdaten gespeicherteMetadaten = createRechnungMetadata(channel, testdaten);
                if (gespeicherteMetadaten == null) {
                    continue;
                }
                Rechnungsdaten geleseneMetadaten =
                        leseRechnungMetadata(channel, gespeicherteMetadaten.getRechnungsId());

                veroeffentlicheZahlungsauftrag(gespeicherteMetadaten);

                // Ausgabe der Rechnungsmetadaten in die Konsole
                System.out.printf(
                        "RechnungClient completed: invoiceId=%s supplier=%s amount=%s %s%n",
                        geleseneMetadaten.getRechnungsId(),
                        geleseneMetadaten.getLieferantenName(),
                        geleseneMetadaten.getGesamtbetragBrutto(),
                        geleseneMetadaten.getWaehrung()
                );
            }
        }
        finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /// Erstellt ein Rechnungsdaten-Objekt alles allen Eintragen der Liste der Mockdaten
    private static Rechnungsdaten createRechnungMetadata(ManagedChannel channel, Rechnungsdaten metadata) {
        CreateRechnungMetadataRequest request = CreateRechnungMetadataRequest.newBuilder()
                .setMetadata(metadata)
                .build();

        // Metadaten an der grpc-Client senden
        RechnungMetadataServiceGrpc.RechnungMetadataServiceBlockingStub stub =
                RechnungMetadataServiceGrpc.newBlockingStub(channel);

        try {
            CreateRechnungMetadataResponse response = stub.createRechnungMetadata(request);
            return response.getMetadata();
        }
        // Exception ausgeben wenn eine ID schon existiert
        catch (StatusRuntimeException exception) {
            if (exception.getStatus().getCode() == Status.Code.ALREADY_EXISTS) {
                System.out.print(exception.getMessage());
                return null;
            }
            throw exception;
        }
    }

    /// Erstellen der Mockdaten für die Rechnungsmetadaten (25 Stück)
    private static List<Rechnungsdaten> createMockRechnungen() {
        List<Rechnungsdaten> rechnungen = new ArrayList<>();

        for (int i = 1; i <= 25; i++) {
            Rechnungsdaten rechnung = Rechnungsdaten.newBuilder()
                    .setLieferantenName("Lieferant " + i + " GmbH")
                    .setRechnungsNummer(String.format("RE-2026-%04d", i))
                    .setRechnungsDatum(String.format("2026-04-%02d", (i % 28) + 1))
                    .setGesamtbetragBrutto(String.format("%d.00", 100 + i * 25))
                    .setWaehrung("EUR")
                    .build();

            rechnungen.add(rechnung);
        }

        // Dublette um RechnungBereitsErfasstException abzufangen
        Rechnungsdaten rechnung = Rechnungsdaten.newBuilder()
                .setLieferantenName("Lieferant " + 1 + " GmbH")
                .setRechnungsNummer(String.format("RE-2026-%04d", 1))
                .setRechnungsDatum(String.format("2026-04-%02d", (1 % 28) + 1))
                .setGesamtbetragBrutto(String.format("%d.00", 100 + 25))
                .setWaehrung("EUR")
                .build();

        rechnungen.add(rechnung);

        return rechnungen;
    }

    /// Die erstellten Metadaten aus dem grpc-Service auslesen
    private static Rechnungsdaten leseRechnungMetadata(ManagedChannel channel, String rechnungsId) {
        GetRechnungMetadataRequest request = GetRechnungMetadataRequest.newBuilder()
                .setRechnungsId(rechnungsId)
                .build();

        RechnungMetadataServiceGrpc.RechnungMetadataServiceBlockingStub stub =
                RechnungMetadataServiceGrpc.newBlockingStub(channel);
        GetRechnungMetadataResponse response = stub.getRechnungMetadata(request);
        return response.getMetadata();
    }

    /// aus den Metadaten einen Zahlungsauftrag erstellen und diesen in die Queue an rabbitMQ schicken
    private static void veroeffentlicheZahlungsauftrag(Rechnungsdaten gespeicherteMetadaten) throws Exception {
        Zahlungsauftrag zahlungsauftrag = Zahlungsauftrag.toZahlungsauftrag(gespeicherteMetadaten);
        try (ZahlungsauftragPublisher publisher = new ZahlungsauftragPublisher()) {
            publisher.publish(zahlungsauftrag);
        }
    }

    /// Umgebungswert für den grpc-Service setzen
    private static String umgebungswert(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
