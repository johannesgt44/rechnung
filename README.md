# Rechnung

Sprint-1-Grundlage fuer die Integrationsarchitektur zur Eingangsrechnungsverarbeitung.

## Komponenten

- `rechnung-metadata-server`: gRPC-Service zum Speichern von Rechnungsmetadaten
- `zahlung-service`: Messaging-Consumer fuer Zahlungsauftraege
- `rechnungs-client`: Client, der Metadaten speichert und Zahlungen veranlasst

## Aktueller Stand

Der gRPC-Vertrag liegt in `src/main/proto/rechnung_metadata.proto`.

Der Server startet den `RechnungMetadataServer` auf Port `50051` und speichert Rechnungsmetadaten zunaechst in-memory.

RabbitMQ wird als Message Broker verwendet. Der Zahlung-Service konsumiert Nachrichten aus der Queue `payment.orders`.

## Demo starten

RabbitMQ starten:

```powershell
docker compose up -d
```

Terminal 1: gRPC-Service starten.

```powershell
.\gradlew.bat run
```

Terminal 2: Zahlung-Service starten.

```powershell
.\gradlew.bat runZahlungService
```

Terminal 3: Client ausfuehren.

```powershell
.\gradlew.bat runRechnungsClient
```

Der Client speichert eine Beispielrechnung ueber gRPC und sendet anschliessend einen Zahlungsauftrag an RabbitMQ. Der Zahlung-Service verarbeitet diese Nachricht und bestaetigt sie.

## Verfuegbare Gradle-Tasks

- `.\gradlew.bat run` startet den Server
- `.\gradlew.bat runZahlungService` startet den Zahlung-Service
- `.\gradlew.bat runRechnungsClient` startet den Rechnungs-Client

## Konfiguration

Die Defaults passen fuer lokale Entwicklung:

- `INVOICE_METADATA_HOST=localhost`
- `INVOICE_METADATA_PORT=50051`
- `RABBITMQ_HOST=localhost`
- `RABBITMQ_PORT=5672`
- `RABBITMQ_USERNAME=guest`
- `RABBITMQ_PASSWORD=guest`
- `PAYMENT_QUEUE=payment.orders`
