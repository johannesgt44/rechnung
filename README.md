// Wurde mit KI erstellt 
# Rechnung

Sprint-1-Grundlage fuer die Integrationsarchitektur zur Eingangsrechnungsverarbeitung.

## Komponenten

- `rechnung-metadata-server`: gRPC-Service zum Speichern von Rechnungsmetadaten
- `zahlung-service`: Messaging-Consumer fuer Zahlungsauftraege
- `rechnungsmetadaten-worker`: Camunda Job Worker, der den gRPC-Metadatenservice aufruft

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

Terminal 3: Camunda Job Worker starten.

```powershell
.\gradlew.bat runRechnungsmetadatenWorker
```

Im Camunda Modeler muss beim Service Task `Rechnungsmetadaten speichern` dieser Job Type eingetragen werden:

```text
rechnung-metadaten-speichern
```

Der Worker liest die Prozessvariablen aus Camunda, ruft intern den gRPC-Service auf und speichert die Rechnungsmetadaten mit der bestehenden Validierung.

Notwendige Prozessvariablen:

```json
{
  "lieferantenName": "Lieferant 1 GmbH",
  "rechnungsNummer": "RE-2026-0001",
  "rechnungsDatum": "2026-04-02",
  "gesamtbetragBrutto": "125.00",
  "waehrung": "EUR"
}
```

## Verfuegbare Gradle-Tasks

- `.\gradlew.bat run` startet den Server
- `.\gradlew.bat runZahlungService` startet den Zahlung-Service
- `.\gradlew.bat runRechnungsmetadatenWorker` startet den Camunda Job Worker

## Konfiguration

Die Defaults passen fuer lokale Entwicklung:

- `INVOICE_METADATA_HOST=localhost`
- `INVOICE_METADATA_PORT=50051`
- `RABBITMQ_HOST=localhost`
- `RABBITMQ_PORT=5672`
- `RABBITMQ_USERNAME=guest`
- `RABBITMQ_PASSWORD=guest`
- `PAYMENT_QUEUE=payment.orders`
