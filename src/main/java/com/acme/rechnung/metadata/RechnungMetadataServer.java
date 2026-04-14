/*Selbstgecoded mit KI als Template*/
package com.acme.rechnung.metadata;

import com.acme.rechnung.repository.RechnungRepository;
import com.acme.rechnung.service.RechnungService;
import com.acme.rechnung.service.RechnungWriteService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public final class RechnungMetadataServer {
    private static final int DEFAULT_PORT = 50051;

    private final Server server;

    private RechnungMetadataServer(int port) {
        RechnungRepository repository = new RechnungRepository();
        RechnungService rechnungService = new RechnungService(repository);
        RechnungWriteService rechnungWriteService = new RechnungWriteService(repository);

        this.server = ServerBuilder.forPort(port)
                .addService(new RechnungMetadataGrpcService(rechnungService, rechnungWriteService))
                .build();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        RechnungMetadataServer rechnungMetadataServer = new RechnungMetadataServer(port);
        rechnungMetadataServer.start();
        rechnungMetadataServer.awaitTermination();
    }

    private void start() throws IOException {
        server.start();
        System.out.printf("InvoiceMetadataService started on port %d%n", server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping InvoiceMetadataService");
            RechnungMetadataServer.this.stop();
        }));
    }

    private void stop() {
        server.shutdown();
    }

    private void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }
}
