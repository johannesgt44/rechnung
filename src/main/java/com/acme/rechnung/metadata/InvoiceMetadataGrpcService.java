package com.acme.rechnung.metadata;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.invoice.v1.RechnungMetadataServiceGrpc;
import com.acme.rechnung.invoice.v1.SaveRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.SaveRechnungMetadataResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

final class InvoiceMetadataGrpcService extends RechnungMetadataServiceGrpc.RechnungMetadataServiceImplBase {
    private final RechnungMetadataRepository repository;

    InvoiceMetadataGrpcService(RechnungMetadataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveRechnungMetadata(
            SaveRechnungMetadataRequest request,
            StreamObserver<SaveRechnungMetadataResponse> responseObserver
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
        SaveRechnungMetadataResponse response = SaveRechnungMetadataResponse.newBuilder()
                .setMetadata(gespeicherteMetadaten)
                .setStatus("SAVED")
                .build();

        System.out.printf("InvoiceMetadata saved " + metadata);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
