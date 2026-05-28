/* Selbstgecoded mit KI als Template, angepasst fuer Camunda-Orchestrierung
*
* https://github.com/camunda-community-hub/C7-C8-workers Template daraus.
* */
package com.acme.rechnung.camunda;

import com.acme.rechnung.invoice.v1.CreateRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.CreateRechnungMetadataResponse;
import com.acme.rechnung.invoice.v1.RechnungMetadataServiceGrpc;
import com.acme.rechnung.invoice.v1.Rechnungsposten;
import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.worker.JobWorker;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Job Worker fuer den Camunda Service Task "Rechnungsmetadaten speichern".
 *
 * In Camunda muss beim Service Task dieser Job Type eingetragen werden:
 * rechnung-metadaten-speichern
 */
public final class RechnungsmetadatenSpeichernWorker {
    private static final String JOB_TYPE = "rechnung-metadaten-speichern";
    private static final String WORKER_NAME = "rechnung-metadaten-worker";

    private RechnungsmetadatenSpeichernWorker() {
    }

    public static void main(String[] args) throws InterruptedException {
        Properties camundaCredentials = ladeCamundaCredentials();

        ManagedChannel grpcChannel = ManagedChannelBuilder
                .forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        RechnungMetadataServiceGrpc.RechnungMetadataServiceBlockingStub rechnungGrpcClient =
                RechnungMetadataServiceGrpc.newBlockingStub(grpcChannel);
        RechnungsmetadatenWorker rechnungsmetadatenWorker = new RechnungsmetadatenWorker(rechnungGrpcClient);

        try (
                CamundaClient camundaClient = CamundaClient.newCloudClientBuilder()
                        .withClusterId(camundaCredentials.getProperty("camunda.client.cloud.cluster-id"))
                        .withClientId(camundaCredentials.getProperty("camunda.client.auth.client-id"))
                        .withClientSecret(camundaCredentials.getProperty("camunda.client.auth.client-secret"))
                        .withRegion(camundaCredentials.getProperty("camunda.client.cloud.region"))
                        .build();
                JobWorker worker = rechnungsmetadatenWorker.open(camundaClient)
        ) {
            System.out.printf("Job Worker gestartet und wartet auf Jobs vom Typ: %s%n", JOB_TYPE);
            new CountDownLatch(1).await();
        } finally {
            grpcChannel.shutdownNow();
        }
    }

    private static final class RechnungsmetadatenWorker extends BaseCamundaWorker {
        private final RechnungMetadataServiceGrpc.RechnungMetadataServiceBlockingStub rechnungGrpcClient;

        private RechnungsmetadatenWorker(
                RechnungMetadataServiceGrpc.RechnungMetadataServiceBlockingStub rechnungGrpcClient
        ) {
            super(WORKER_NAME);
            this.rechnungGrpcClient = rechnungGrpcClient;
        }

        @Override
        String getType() {
            return JOB_TYPE;
        }

        @Override
        void executeWorker(JobClient jobClient, JobInformation jobInformation) {
            System.out.println("Camunda Job empfangen: rechnung-metadaten-speichern");
            Rechnungsdaten rechnung = rechnungsdatenAus(jobInformation);

            try {
                CreateRechnungMetadataResponse response = rechnungGrpcClient.createRechnungMetadata(
                        CreateRechnungMetadataRequest.newBuilder()
                                .setMetadata(rechnung)
                                .build()
                );

                Rechnungsdaten gespeicherteRechnung = response.getMetadata();
                complete(jobClient, jobInformation, Map.of(
                        "rechnungsId", gespeicherteRechnung.getRechnungsNummer(),
                        "rechnungsNummer", gespeicherteRechnung.getRechnungsNummer(),
                        "metadatenStatus", response.getStatus()
                ));

                System.out.printf("Rechnungsmetadaten gespeichert: %s%n", gespeicherteRechnung.getRechnungsNummer());
            } catch (StatusRuntimeException exception) {
                fail(jobClient, jobInformation, exception.getStatus().getDescription());
            }
        }
    }

    private static Rechnungsdaten rechnungsdatenAus(JobInformation jobInformation) {
        return Rechnungsdaten.newBuilder()
                .setLieferantenName(jobInformation.getStringVariable("lieferantenName"))
                .setLieferantenNummer(jobInformation.getStringVariable("lieferantenNummer"))
                .setRechnungsNummer(jobInformation.getStringVariable("rechnungsNummer"))
                .setRechnungsDatum(jobInformation.getStringVariable("rechnungsDatum"))
                .setZahlungsziel(jobInformation.getStringVariable("zahlungsziel"))
                .setBemerkungen(jobInformation.getStringVariable("bemerkungen"))
                .setGesamtbetragNetto(jobInformation.getStringVariable("gesamtbetragNetto"))
                .setGesamtbetragBrutto(jobInformation.getStringVariable("gesamtbetragBrutto"))
                .setSteuerbetrag(jobInformation.getStringVariable("steuerbetrag"))
                .setWaehrung(jobInformation.getStringVariable("waehrung"))
                .addAllRechnungsposten(rechnungspostenAus(jobInformation.getVariable("rechnungsposten")))
                .build();
    }

    private static Iterable<Rechnungsposten> rechnungspostenAus(Object value) {
        java.util.List<Rechnungsposten> posten = new java.util.ArrayList<>();
        if (!(value instanceof Iterable<?> eintraege)) {
            return posten;
        }

        for (Object eintrag : eintraege) {
            if (eintrag instanceof Map<?, ?> postenMap) {
                posten.add(Rechnungsposten.newBuilder()
                        .setPosition(zahl(postenMap.get("position")))
                        .setBeschreibung(postenWert(postenMap, "beschreibung"))
                        .setMenge(postenWert(postenMap, "menge"))
                        .setEinheit(postenWert(postenMap, "einheit"))
                        .setEinzelpreisNetto(postenWert(postenMap, "einzelpreisNetto"))
                        .setSteuerProzent(postenWert(postenMap, "steuerProzent"))
                        .build());
            }
        }
        return posten;
    }

    private static String postenWert(Map<?, ?> variablen, String name) {
        Object value = variablen.get(name);
        return value == null ? "" : value.toString();
    }

    private static int zahl(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || value.toString().isBlank()) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }

    private static Properties ladeCamundaCredentials() {
        Path credentialsPath = Path.of(
                System.getProperty("user.dir"),
                "CamundaClientCredentials.properties"
        );

        Properties properties = new Properties();
        try (var inputStream = Files.newInputStream(credentialsPath)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Camunda-Credentials-Datei konnte nicht gelesen werden: " + credentialsPath, exception);
        }

        pruefeProperty(properties, "camunda.client.cloud.cluster-id");
        pruefeProperty(properties, "camunda.client.cloud.region");
        pruefeProperty(properties, "camunda.client.auth.client-id");
        pruefeProperty(properties, "camunda.client.auth.client-secret");
        return properties;
    }

    private static void pruefeProperty(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Camunda-Credentials-Datei enthaelt keinen Wert fuer: " + name);
        }
    }
}
