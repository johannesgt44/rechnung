/* Selbstgecoded mit KI als Template, angepasst fuer Camunda-Orchestrierung */
package com.acme.rechnung.camunda;

import com.acme.rechnung.invoice.v1.CreateRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.CreateRechnungMetadataResponse;
import com.acme.rechnung.invoice.v1.RechnungMetadataServiceGrpc;
import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.worker.JobWorker;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Job Worker fuer den Camunda Service Task "Rechnungsmetadaten speichern".
 *
 * In Camunda muss beim Service Task dieser Job Type eingetragen werden:
 * rechnungsmetadaten-speichern
 */
public final class RechnungsmetadatenSpeichernWorker {
    private static final String JOB_TYPE = "rechnungsmetadaten-speichern";

    private RechnungsmetadatenSpeichernWorker() {
    }

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel grpcChannel = ManagedChannelBuilder
                .forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        RechnungMetadataServiceGrpc.RechnungMetadataServiceBlockingStub rechnungGrpcClient =
                RechnungMetadataServiceGrpc.newBlockingStub(grpcChannel);

        try (
                CamundaClient camundaClient = CamundaClient.newClientBuilder()
                        .grpcAddress(URI.create("http://localhost:26500"))
                        .restAddress(URI.create("http://localhost:8080"))
                        .build();
                JobWorker worker = camundaClient.newWorker()
                        .jobType(JOB_TYPE)
                        .handler((jobClient, job) -> bearbeiteJob(jobClient, job, rechnungGrpcClient))
                        .open()
        ) {
            System.out.printf("Job Worker gestartet und wartet auf Jobs vom Typ: %s%n", JOB_TYPE);
            new CountDownLatch(1).await();
        } finally {
            grpcChannel.shutdownNow();
        }
    }

    private static void bearbeiteJob(
            JobClient jobClient,
            ActivatedJob job,
            RechnungMetadataServiceGrpc.RechnungMetadataServiceBlockingStub rechnungGrpcClient
    ) {
        Map<String, Object> variablen = job.getVariablesAsMap();

        Rechnungsdaten rechnung = Rechnungsdaten.newBuilder()
                .setLieferantenName(wert(variablen, "lieferantenName"))
                .setRechnungsNummer(wert(variablen, "rechnungsNummer"))
                .setRechnungsDatum(wert(variablen, "rechnungsDatum"))
                .setGesamtbetragBrutto(wert(variablen, "gesamtbetragBrutto"))
                .setWaehrung(wert(variablen, "waehrung"))
                .build();

        try {
            CreateRechnungMetadataResponse response = rechnungGrpcClient.createRechnungMetadata(
                    CreateRechnungMetadataRequest.newBuilder()
                            .setMetadata(rechnung)
                            .build()
            );

            Rechnungsdaten gespeicherteRechnung = response.getMetadata();
            jobClient.newCompleteCommand(job.getKey())
                    .variables(Map.of(
                            "rechnungsId", gespeicherteRechnung.getRechnungsId(),
                            "metadatenStatus", response.getStatus()
                    ))
                    .send()
                    .join();

            System.out.printf("Rechnungsmetadaten gespeichert: %s%n", gespeicherteRechnung.getRechnungsId());
        } catch (StatusRuntimeException exception) {
            jobClient.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage(exception.getStatus().getDescription())
                    .send()
                    .join();
        }
    }

    private static String wert(Map<String, Object> variablen, String name) {
        Object value = variablen.get(name);
        return value == null ? "" : value.toString();
    }
}
