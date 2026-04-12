package com.acme.rechnung.metadata;

import com.acme.rechnung.invoice.v1.InvoiceMetadataServiceGrpc;
import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.invoice.v1.SaveInvoiceMetadataRequest;
import com.acme.rechnung.invoice.v1.SaveInvoiceMetadataResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

final class InvoiceMetadataGrpcService extends InvoiceMetadataServiceGrpc.InvoiceMetadataServiceImplBase {
    private final RechnungMetadataRepository repository;

    InvoiceMetadataGrpcService(RechnungMetadataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveInvoiceMetadata(
            SaveInvoiceMetadataRequest request,
            StreamObserver<SaveInvoiceMetadataResponse> responseObserver
    ) {
        if (!request.hasMetadata()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("metadata is required")
                    .asRuntimeException());
            return;
        }

        Rechnungsdaten metadata = request.getMetadata();
        if (metadata.getLieferantenName().isBlank() || metadata.getRechnungsNummer().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("lieferanten_name und rechnungs_nummer sind erforderlich")
                    .asRuntimeException());
            return;
        }

        Rechnungsdaten gespeicherteMetadaten = repository.save(metadata);
        SaveInvoiceMetadataResponse response = SaveInvoiceMetadataResponse.newBuilder()
                .setMetadata(gespeicherteMetadaten)
                .setStatus("SAVED")
                .build();

        System.out.printf("InvoiceMetadata saved " + metadata);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
