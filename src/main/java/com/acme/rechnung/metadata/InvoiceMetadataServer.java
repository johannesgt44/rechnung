package com.acme.rechnung.metadata;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public final class InvoiceMetadataServer {
    //Standardport für gRPC
    private static final int DEFAULT_PORT = 50051;

    //Eigentliches gRPC-Server-Objekt wird deklariert
    private final Server server;

    //Server wird zusammengebaut (Konstruktor)
    private InvoiceMetadataServer(int port) {
        this.server = ServerBuilder.forPort(port)
                //An diesen Service werden Anfragen weitergeleitet und das Repository wird gleich mitgegeben (Dependency Injection)
                .addService(new InvoiceMetadataGrpcService(new InMemoryInvoiceMetadataRepository()))
                .build();
    }

    //Startpunkt der Anwendung
    public static void main(String[] args) throws IOException, InterruptedException {
        //Falls kein Port angegeben wurde, nimm den Standartport
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        //Server-Instanz erstellen
        InvoiceMetadataServer invoiceMetadataServer = new InvoiceMetadataServer(port);
        //Server starten
        invoiceMetadataServer.start();
        //Leitung offen halten, ansonsten beendet Programm sofort
        invoiceMetadataServer.awaitTermination();
    }

    //Startet den Server-Prozess und öffnet den Port
    private void start() throws IOException {
        server.start();
        System.out.printf("InvoiceMetadataService startet auf Port %d%n", server.getPort());

        //Falls jemand das Programm beendet, wird der Server dennoch sauber heruntergefahren
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stoppen des InvoiceMetadataService");
            InvoiceMetadataServer.this.stop();
        }));
    }

    //Stoppt den Server und beendet alle aktiven Verbindungen
    private void stop() {
        server.shutdown();
    }

    //Lässt das Programm in dieser Zeile warten, solange der Server läuft
    private void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }
}
