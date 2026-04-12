package com.acme.rechnung.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.acme.rechnung.invoice.v1.CreateRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.CreateRechnungMetadataResponse;
import com.acme.rechnung.invoice.v1.GetRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.GetRechnungMetadataResponse;
import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.invoice.v1.UpdateRechnungMetadataRequest;
import com.acme.rechnung.invoice.v1.UpdateRechnungMetadataResponse;
import com.acme.rechnung.repository.RechnungRepository;
import com.acme.rechnung.service.RechnungService;
import com.acme.rechnung.service.RechnungWriteService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RechnungMetadataGrpcServiceTest {
    private final RechnungRepository repository = new RechnungRepository();
    private final RechnungMetadataGrpcService service =
            new RechnungMetadataGrpcService(
                    new RechnungService(repository),
                    new RechnungWriteService(repository)
            );

    @Test
    void createUndGetFunktionierenFuerGespeicherteRechnung() {
        TestStreamObserver<CreateRechnungMetadataResponse> createObserver = new TestStreamObserver<>();
        service.createRechnungMetadata(
                CreateRechnungMetadataRequest.newBuilder()
                        .setMetadata(beispielRechnung().build())
                        .build(),
                createObserver
        );

        CreateRechnungMetadataResponse createResponse = createObserver.ersteAntwort();
        TestStreamObserver<GetRechnungMetadataResponse> getObserver = new TestStreamObserver<>();
        service.getRechnungMetadata(
                GetRechnungMetadataRequest.newBuilder()
                        .setRechnungsId(createResponse.getMetadata().getRechnungsId())
                        .build(),
                getObserver
        );

        assertEquals("CREATED", createResponse.getStatus());
        assertEquals(createResponse.getMetadata(), getObserver.ersteAntwort().getMetadata());
    }

    @Test
    void createLiefertAlreadyExistsBeiDubletten() {
        service.createRechnungMetadata(
                CreateRechnungMetadataRequest.newBuilder()
                        .setMetadata(beispielRechnung().build())
                        .build(),
                new TestStreamObserver<>()
        );

        TestStreamObserver<CreateRechnungMetadataResponse> observer = new TestStreamObserver<>();
        service.createRechnungMetadata(
                CreateRechnungMetadataRequest.newBuilder()
                        .setMetadata(beispielRechnung().build())
                        .build(),
                observer
        );

        StatusRuntimeException exception = observer.fehler();
        assertEquals(Status.Code.ALREADY_EXISTS, exception.getStatus().getCode());
    }

    @Test
    void updateLiefertNotFoundFuerUnbekannteRechnung() {
        TestStreamObserver<UpdateRechnungMetadataResponse> observer = new TestStreamObserver<>();
        service.updateRechnungMetadata(
                UpdateRechnungMetadataRequest.newBuilder()
                        .setMetadata(beispielRechnung().setRechnungsId("fehlt").build())
                        .build(),
                observer
        );

        StatusRuntimeException exception = observer.fehler();
        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }

    private static Rechnungsdaten.Builder beispielRechnung() {
        return Rechnungsdaten.newBuilder()
                .setLieferantenName("Muster Lieferant GmbH")
                .setRechnungsNummer("RE-2026-0001")
                .setRechnungsDatum("2026-04-07")
                .setGesamtbetragBrutto("1190.00")
                .setWaehrung("EUR");
    }

    private static final class TestStreamObserver<T> implements StreamObserver<T> {
        private final AtomicReference<T> antwort = new AtomicReference<>();
        private final AtomicReference<Throwable> fehler = new AtomicReference<>();

        @Override
        public void onNext(T value) {
            antwort.compareAndSet(null, value);
        }

        @Override
        public void onError(Throwable t) {
            fehler.compareAndSet(null, t);
        }

        @Override
        public void onCompleted() {
        }

        T ersteAntwort() {
            T value = antwort.get();
            assertNotNull(value);
            return value;
        }

        StatusRuntimeException fehler() {
            Throwable throwable = fehler.get();
            assertNotNull(throwable);
            return assertInstanceOf(StatusRuntimeException.class, throwable);
        }
    }
}
