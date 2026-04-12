package com.acme.rechnung.metadata;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public final class InvoiceMetadataServer {
    private static final int DEFAULT_PORT = 50051;

    private final Server server;

    private InvoiceMetadataServer(int port) {
        this.server = ServerBuilder.forPort(port)
                .addService(new InvoiceMetadataGrpcService(new InMemoryRechnungMetadataRepository()))
                .build();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        InvoiceMetadataServer invoiceMetadataServer = new InvoiceMetadataServer(port);
        invoiceMetadataServer.start();
        invoiceMetadataServer.awaitTermination();
    }

    private void start() throws IOException {
        server.start();
        System.out.printf("InvoiceMetadataService started on port %d%n", server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping InvoiceMetadataService");
            InvoiceMetadataServer.this.stop();
        }));
    }

    private void stop() {
        server.shutdown();
    }

    private void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }
}
