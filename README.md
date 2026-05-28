// Wurde mit KI erstellt 
# Rechnung

Demo-Projekt zur Eingangsrechnungsverarbeitung mit gRPC, RabbitMQ und Camunda 8 Cloud.

## Komponenten

- `rechnung-metadata-server`: gRPC-Service zum Speichern von Rechnungen und Rechnungsposten
- `zahlung-service`: Messaging-Consumer fuer Zahlungsauftraege
- `rechnungsmetadaten-worker`: Camunda Job Worker, der Camunda Cloud Jobs abholt und den gRPC-Service aufruft

## Aktueller Stand

Der gRPC-Vertrag liegt in `src/main/proto/rechnung_metadata.proto`.

Der Server startet den `RechnungMetadataServer` auf Port `50051` und speichert Rechnungen zunaechst in-memory. Die `rechnungsNummer` wird dabei als interner Schluessel verwendet.

RabbitMQ wird als Message Broker verwendet. Der Zahlung-Service konsumiert Nachrichten aus der Queue `payment.orders`.

Der Camunda Worker orientiert sich am Worker-Pattern aus `camunda-community-hub/C7-C8-workers`: ein Worker hat einen Job Type, liest Prozessvariablen, fuehrt Fachlogik aus und beendet den Job mit `complete` oder `fail`.

## Camunda Cloud

Der Service Task `Rechnungsmetadaten speichern` muss diesen Job Type verwenden:

```text
rechnung-metadaten-speichern
```

Die Datei `CamundaClientCredentials.properties` muss im Projektordner liegen. Sie enthaelt die Zugangsdaten fuer Camunda Cloud und wird nicht in Git versioniert.

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

Wichtige Prozessvariablen:

```json
{
  "lieferantenName": "Lieferant 1 GmbH",
  "lieferantenNummer": "L-1001",
  "rechnungsNummer": "RE-2026-0001",
  "rechnungsDatum": "2026-04-02",
  "zahlungsziel": "14 Tage netto",
  "bemerkungen": "optional",
  "gesamtbetragNetto": "100.00",
  "gesamtbetragBrutto": "125.00",
  "steuerbetrag": "25.00",
  "waehrung": "EUR",
  "rechnungsposten": [
    {
      "position": 1,
      "beschreibung": "Beispielposition",
      "menge": "1",
      "einheit": "Stk.",
      "einzelpreisNetto": "100.00",
      "steuerProzent": "19"
    }
  ]
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
