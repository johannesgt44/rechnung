# Rechnung

Sprint-1-Grundlage fuer die Integrationsarchitektur zur Eingangsrechnungsverarbeitung.

## Komponenten

- `invoice-metadata-service`: gRPC-Service zum Speichern von Rechnungsmetadaten
- `payment-service`: Messaging-Consumer fuer Zahlungsauftraege
- `invoice-client`: Client, der Metadaten speichert und Zahlungen veranlasst

## Aktueller Stand

Der gRPC-Vertrag liegt in `src/main/proto/invoice_metadata.proto`.

Der Server startet den `InvoiceMetadataService` auf Port `50051` und speichert Rechnungsmetadaten zunaechst in-memory.

RabbitMQ wird als Message Broker verwendet. Der Payment-Service konsumiert Nachrichten aus der Queue `payment.orders`.

## Demo starten

RabbitMQ starten:

```powershell
docker compose up -d
```

Terminal 1: gRPC-Service starten.

```powershell
.\gradlew.bat run
```

Terminal 2: Payment-Service starten.

```powershell
.\gradlew.bat runPaymentService
```

Terminal 3: Client ausfuehren.

```powershell
.\gradlew.bat runInvoiceClient
```

Der Client speichert eine Beispielrechnung ueber gRPC und sendet anschliessend einen Zahlungsauftrag an RabbitMQ. Der Payment-Service verarbeitet diese Nachricht und bestaetigt sie.

## Konfiguration

Die Defaults passen fuer lokale Entwicklung:

- `INVOICE_METADATA_HOST=localhost`
- `INVOICE_METADATA_PORT=50051`
- `RABBITMQ_HOST=localhost`
- `RABBITMQ_PORT=5672`
- `RABBITMQ_USERNAME=guest`
- `RABBITMQ_PASSWORD=guest`
- `PAYMENT_QUEUE=payment.orders`
