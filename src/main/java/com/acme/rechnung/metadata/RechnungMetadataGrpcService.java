/*Selbstgecoded mit KI als Template, überarbeitet mit KI, eigene Umstrukturierung*/
package com.acme.rechnung.metadata;

import com.acme.rechnung.invoice.v1.CreateRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.CreateRechnungMetadataResponse;
import com.acme.rechnung.invoice.v1.GetRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.GetRechnungMetadataResponse;
import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.invoice.v1.RechnungMetadataServiceGrpc;
import com.acme.rechnung.service.RechnungBereitsErfasstException;
import com.acme.rechnung.service.RechnungNichtGefundenException;
import com.acme.rechnung.service.RechnungService;
import com.acme.rechnung.service.RechnungWriteService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

final class RechnungMetadataGrpcService extends RechnungMetadataServiceGrpc.RechnungMetadataServiceImplBase {
    /* Abhängigkeiten zwischen Service und Grp-Service herstellen, um auch Fehlerfälle abzudecken.*/
    private final RechnungService rechnungService;
    private final RechnungWriteService rechnungWriteService;

    /** Constructor Injection durch Konstruktor*/
    RechnungMetadataGrpcService(RechnungService rechnungService, RechnungWriteService rechnungWriteService) {
        this.rechnungService = rechnungService;
        this.rechnungWriteService = rechnungWriteService;
    }

    /** Methode zum Erstellen von Rechnungen, mit der Request welche vom Client und einem Objekt aus
     * StreamObserver<CreateRechnungMetadataResponse>, welches durch die generierte Grpc Klasse vorgegeben
     * wird, um damit mit dem Client kommunizieren zu können.
     */
    @Override
    public void createRechnungMetadata(
            CreateRechnungMetadataRequest request,
            StreamObserver<CreateRechnungMetadataResponse> responseObserver
    ) {
        /* überprüft durch generierte Funktion, ob die Metadaten gesetzt sind, wenn nicht soll ein Fehler
        * an den Client geschickt werden.
        */
        if (!request.hasMetadata()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("metadata ist erforderlich")
                    .asRuntimeException());
            return;
        }

        try {
            /* Methodenaufruf zum Erzeugen des Rechnungsobjektes, durch WriteService, mit enthaltener
            * Fehlerfallabdeckung.
            */
            Rechnungsdaten gespeicherteMetadaten = rechnungWriteService.create(request.getMetadata());
            /* Erstellen der Antwort, zusammen mit den Metadaten, anschließend Antwort dem Client
            * zuschicken und Antwort als "fertig" deklarieren (durch onCompleted()).*/
            CreateRechnungMetadataResponse response = CreateRechnungMetadataResponse.newBuilder()
                    .setMetadata(gespeicherteMetadaten)
                    .setStatus("CREATED")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            /* Fehler fangen welche im WriteService geschmissen werden.*/
        } catch (RechnungBereitsErfasstException exception) {
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        }
    }

    /** Methode zur Abfrage von Rechnungsmetadaten (GET-Request) mit der Request welche vom Client und einem Objekt aus
     * StreamObserver<CreateRechnungMetadataResponse>, welches durch die generierte Grpc Klasse vorgegeben
     * wird, um damit mit dem Client kommunizieren zu können. */
    @Override
    public void getRechnungMetadata(
            GetRechnungMetadataRequest request,
            StreamObserver<GetRechnungMetadataResponse> responseObserver
    ) {
        /* Überprüft, ob bei der Request des Clients eine ID des zu suchenden Rechnungsobjektes
        * angegeben wurde, im Fehlerfall dies dem Client mitteilen.
        */
        if (request.getRechnungsId().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("rechnungs_id ist erforderlich")
                    .asRuntimeException());
            return;
        }

        /* Methodenaufruf zum Suchen des Rechnungsobjektes, durch Service, mit enthaltener
         * Fehlerfallabdeckung.
         */
        try {
            Rechnungsdaten metadata = rechnungService.findById(request.getRechnungsId());

            /* Erstellen der Antwort, zusammen mit den Metadaten, anschließend Antwort dem Client
             * zuschicken und Antwort als "fertig" deklarieren (durch onCompleted()).*/
            GetRechnungMetadataResponse response = GetRechnungMetadataResponse.newBuilder()
                    .setMetadata(metadata)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (RechnungNichtGefundenException exception) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        }
    }
}
