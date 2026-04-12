package com.acme.rechnung.metadata;

import com.acme.rechnung.invoice.v1.CreateRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.CreateRechnungMetadataResponse;
import com.acme.rechnung.invoice.v1.GetRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.GetRechnungMetadataResponse;
import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.invoice.v1.RechnungMetadataServiceGrpc;
import com.acme.rechnung.invoice.v1.UpdateRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.UpdateRechnungMetadataResponse;
import com.acme.rechnung.service.RechnungBereitsErfasstException;
import com.acme.rechnung.service.RechnungNichtGefundenException;
import com.acme.rechnung.service.RechnungService;
import com.acme.rechnung.service.RechnungWriteService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

final class RechnungMetadataGrpcService extends RechnungMetadataServiceGrpc.RechnungMetadataServiceImplBase {
    private final RechnungService rechnungService;
    private final RechnungWriteService rechnungWriteService;

    RechnungMetadataGrpcService(RechnungService rechnungService, RechnungWriteService rechnungWriteService) {
        this.rechnungService = rechnungService;
        this.rechnungWriteService = rechnungWriteService;
    }

    @Override
    public void createRechnungMetadata(
            CreateRechnungMetadataRequest request,
            StreamObserver<CreateRechnungMetadataResponse> responseObserver
    ) {
        if (!request.hasMetadata()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("metadata ist erforderlich")
                    .asRuntimeException());
            return;
        }

        try {
            Rechnungsdaten gespeicherteMetadaten = rechnungWriteService.create(request.getMetadata());
            CreateRechnungMetadataResponse response = CreateRechnungMetadataResponse.newBuilder()
                    .setMetadata(gespeicherteMetadaten)
                    .setStatus("CREATED")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
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

    @Override
    public void getRechnungMetadata(
            GetRechnungMetadataRequest request,
            StreamObserver<GetRechnungMetadataResponse> responseObserver
    ) {
        if (request.getRechnungsId().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("rechnungs_id ist erforderlich")
                    .asRuntimeException());
            return;
        }

        try {
            Rechnungsdaten metadata = rechnungService.findById(request.getRechnungsId());
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

    @Override
    public void updateRechnungMetadata(
            UpdateRechnungMetadataRequest request,
            StreamObserver<UpdateRechnungMetadataResponse> responseObserver
    ) {
        if (!request.hasMetadata()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("metadata ist erforderlich")
                    .asRuntimeException());
            return;
        }

        try {
            Rechnungsdaten gespeicherteMetadaten = rechnungWriteService.update(request.getMetadata());
            UpdateRechnungMetadataResponse response = UpdateRechnungMetadataResponse.newBuilder()
                    .setMetadata(gespeicherteMetadaten)
                    .setStatus("UPDATED")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (RechnungBereitsErfasstException exception) {
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        } catch (RechnungNichtGefundenException exception) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        }
    }
}
