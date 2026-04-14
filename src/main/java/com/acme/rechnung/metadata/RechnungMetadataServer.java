package com.acme.rechnung.metadata;

import com.acme.rechnung.repository.RechnungRepository;
import com.acme.rechnung.service.RechnungService;
import com.acme.rechnung.service.RechnungWriteService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

/**
 * Haupteinstiegspunkt
 */
public final class RechnungMetadataServer {

    //Standart-Port wird gesetzt
    private static final int DEFAULT_PORT = 50051;

    private final Server server;

    //Konstruktor um den Server zusammenzubauen inkl. Abhängigkeiten
    private RechnungMetadataServer(int port) {
        //Repository als "Datenlager" erstellen
        RechnungRepository repository = new RechnungRepository();
        //Service zum Lesen von Rechnungen wird initialisiert
        RechnungService rechnungService = new RechnungService(repository);
        //Service zum Schreiben von Rechnungen wird initialisiert
        RechnungWriteService rechnungWriteService = new RechnungWriteService(repository);

        //Server zusammenbauen
        this.server = ServerBuilder.forPort(port)
                .addService(new RechnungMetadataGrpcService(rechnungService, rechnungWriteService))
                .build();
    }

    /**
     *Startpunkt der Anwendung
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        //Prüfen, ob ein Port angegeben wurde. Ansonsten Standart-Port verwenden
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
