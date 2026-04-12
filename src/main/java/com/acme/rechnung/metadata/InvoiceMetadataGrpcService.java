package com.acme.rechnung.metadata;

import com.acme.rechnung.invoice.v1.InvoiceMetadata;
import com.acme.rechnung.invoice.v1.InvoiceMetadataServiceGrpc;
import com.acme.rechnung.invoice.v1.SaveInvoiceMetadataRequest;
import com.acme.rechnung.invoice.v1.SaveInvoiceMetadataResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

//Basisklasse die mit .proto-Datei erstellt wurde, wird um Repository erweitert erweitert
final class InvoiceMetadataGrpcService extends InvoiceMetadataServiceGrpc.InvoiceMetadataServiceImplBase {
    private final InvoiceMetadataRepository repository;

    InvoiceMetadataGrpcService(InvoiceMetadataRepository repository) {
        this.repository = repository;
    }


    @Override
    //Befehl, der vom Client aufgerufen wird. request: Daten die mitgeschickt werden. responseObserver: Rückkanal für Antworten
    public void saveInvoiceMetadata(
            SaveInvoiceMetadataRequest request,
            StreamObserver<SaveInvoiceMetadataResponse> responseObserver
    ) {
        //Validerung: Es wird geprüft ob die Request Daten korrekt sind
        if (!request.hasMetadata()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Metadaten werden benötigt")
                    .asRuntimeException());
            return;
        }

        InvoiceMetadata metadata = request.getMetadata();
        if (metadata.getSupplierName().isBlank() || metadata.getInvoiceNumber().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("lieferanten_name und rechnungs_nummer werden benötigt")
                    .asRuntimeException());
            return;
        }

        //Wenn die Daten korrekt sind, werden sie im Repository gespeichert
        InvoiceMetadata storedMetadata = repository.save(metadata);

        //Das Antwort-Paket/Objekt wird zusammengebaut(Builder-Pattern) und
        SaveInvoiceMetadataResponse response = SaveInvoiceMetadataResponse.newBuilder()
                .setMetadata(storedMetadata)
                .setStatus("SAVED")
                .build();

        //Ausgabe für Server als Bestätigung
        System.out.println("InvoiceMetadata gespeichert " + metadata );

        //Sendet die Antwort zurück an den Client
        responseObserver.onNext(response);
        //Schließt den aktuellen RPC-Aufruf -> Ende der Übertragung
        responseObserver.onCompleted();
    }
}
