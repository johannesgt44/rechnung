package com.acme.rechnung.metadata;

import com.acme.rechnung.invoice.v1.InvoiceMetadata;
import com.acme.rechnung.invoice.v1.InvoiceMetadataServiceGrpc;
import com.acme.rechnung.invoice.v1.SaveInvoiceMetadataRequest;
import com.acme.rechnung.invoice.v1.SaveInvoiceMetadataResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

final class InvoiceMetadataGrpcService extends InvoiceMetadataServiceGrpc.InvoiceMetadataServiceImplBase {
    private final InvoiceMetadataRepository repository;

    InvoiceMetadataGrpcService(InvoiceMetadataRepository repository) {
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

        InvoiceMetadata metadata = request.getMetadata();
        if (metadata.getSupplierName().isBlank() || metadata.getInvoiceNumber().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("supplier_name and invoice_number are required")
                    .asRuntimeException());
            return;
        }

        InvoiceMetadata storedMetadata = repository.save(metadata);
        SaveInvoiceMetadataResponse response = SaveInvoiceMetadataResponse.newBuilder()
                .setMetadata(storedMetadata)
                .setStatus("SAVED")
                .build();

        System.out.printf("InvoiceMetadata saved " + metadata );

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
