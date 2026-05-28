# BPMN-Briefing fuer das Rechnungsprojekt

Dieses Dokument ist als Input fuer ChatGPT gedacht, um daraus ein passendes BPMN-2.0-Modell fuer Camunda abzuleiten. Der aktuelle Code soll dabei nicht automatisch veraendert werden. Es geht zuerst um fachliches und technisches Prozessverstaendnis.

## Projektueberblick

Das Projekt `rechnung` ist eine Java/Gradle-Demo fuer Eingangsrechnungsverarbeitung in einer Integrationsarchitektur.

Aktuelle technische Bausteine:

- Java-Anwendung mit Gradle
- gRPC-Service fuer Rechnungsmetadaten
- In-Memory-Repository fuer Rechnungsdaten
- separater Zahlungsservice im Projekt `zahlungssystem`
- Camunda Worker fuer Rechnungsmetadaten

Wichtig: Der Zahlungsauftrag wird nicht mehr in diesem Projekt erzeugt oder verarbeitet. Das BPMN-Modell soll den Zahlungsauftrag ueber den separaten Zahlungsservice im Projekt `zahlungssystem` an RabbitMQ anbinden.

## Relevante Dateien

- `src/main/proto/rechnung_metadata.proto`: gRPC-Vertrag fuer Rechnungsmetadaten
- `src/main/java/com/acme/rechnung/metadata/RechnungMetadataServer.java`: Startet den gRPC-Server auf Port `50051`
- `src/main/java/com/acme/rechnung/metadata/RechnungMetadataGrpcService.java`: gRPC-Endpunkte und Fehler-Mapping
- `src/main/java/com/acme/rechnung/service/RechnungWriteService.java`: Validierung und Dublettenpruefung beim Erfassen
- `src/main/java/com/acme/rechnung/service/RechnungService.java`: Lesender Zugriff auf Rechnungsdaten
- `src/main/java/com/acme/rechnung/repository/RechnungRepository.java`: In-Memory-Speicher und Fachschluessel-Dublettenlogik
- `src/main/java/com/acme/rechnung/camunda/RechnungsmetadatenSpeichernWorker.java`: Camunda Worker fuer Rechnungsmetadaten
- `../zahlungssystem/src/main/java/com/acme/zahlung/ZahlungsauftragCamundaWorker.java`: Camunda Worker, der Zahlungsauftraege nach RabbitMQ sendet
- `../zahlungssystem/src/main/java/com/acme/zahlung/ZahlungsServiceWorker.java`: Konsumiert Zahlungsauftraege aus RabbitMQ und bestaetigt/verwirft Nachrichten
- `docker-compose.yml`: Startet RabbitMQ mit Management UI

## Aktueller Demo-Ablauf

1. RabbitMQ wird per Docker gestartet.
2. Der gRPC-Metadatenserver wird gestartet.
3. Der Rechnungsmetadaten-Worker speichert Rechnungsdaten ueber den gRPC-Server.
4. Der Zahlungsauftrag-Worker im Projekt `zahlungssystem` erzeugt aus Camunda-Prozessvariablen einen `Zahlungsauftrag`.
5. Der Zahlungsauftrag-Worker sendet den Zahlungsauftrag als JSON-Nachricht an RabbitMQ in die Queue `payment.orders`.
6. Der Zahlungsservice-Worker im Projekt `zahlungssystem` empfaengt die Nachricht, deserialisiert sie, gibt eine Erfolgsmeldung aus und bestaetigt die Nachricht mit `basicAck`.
7. Falls der Worker eine Nachricht nicht verarbeiten kann, sendet er `basicNack` mit `requeue=false`; die Nachricht wird also nicht erneut eingereiht.

## Fachliche Datenobjekte

### Rechnungsdaten

Aus dem gRPC-Proto:

- `rechnungs_id`: technische ID der Rechnung, wird bei Bedarf automatisch als UUID erzeugt
- `lieferanten_name`: Lieferant, Pflichtfeld
- `rechnungs_nummer`: Rechnungsnummer, Pflichtfeld
- `rechnungs_datum`: Rechnungsdatum, aktuell nicht validiert
- `gesamtbetrag_brutto`: Bruttobetrag als String, Pflichtfeld
- `waehrung`: Waehrung, Pflichtfeld

### Zahlungsauftrag

Aus dem Zahlungsservice im Projekt `zahlungssystem`:

- `zahlungsId`: technische ID des Zahlungsauftrags, wird als UUID erzeugt
- `rechnungsId`: ID der gespeicherten Rechnung
- `lieferantenName`
- `rechnungsNummer`
- `betrag`: uebernommen aus `gesamtbetrag_brutto`
- `waehrung`

## Geschaeftsregeln und Validierung

Beim Erfassen einer Rechnung gelten aktuell diese Regeln:

- `metadata` muss im gRPC-Request vorhanden sein.
- `lieferanten_name` darf nicht leer sein.
- `rechnungs_nummer` darf nicht leer sein.
- `gesamtbetrag_brutto` darf nicht leer sein.
- `waehrung` darf nicht leer sein.
- Dubletten werden ueber einen Fachschluessel erkannt: normalisierter `lieferanten_name` plus normalisierte `rechnungs_nummer`.
- Ist `rechnungs_id` leer, erzeugt das Repository eine UUID.
- `rechnungs_datum` wird aktuell zwar gespeichert, aber nicht fachlich validiert.

Beim Lesen einer Rechnung:

- `rechnungs_id` muss vorhanden sein.
- Falls keine Rechnung gefunden wird, entsteht ein fachlicher Fehler `NOT_FOUND`.

Beim Zahlungsauftrag:

- Der Zahlungsauftrag wird nur erzeugt, wenn das Speichern der Rechnungsmetadaten erfolgreich war.
- Die aktuelle Demo enthaelt noch keine fachliche Freigabe, keine Zahlungslimits, keine Vier-Augen-Pruefung und keinen echten Bank-/ERP-Adapter.

## Technische Schnittstellen

### gRPC-Service

Service: `RechnungMetadataService`

Operationen:

- `CreateRechnungMetadata(CreateRechnungMetadataRequest) -> CreateRechnungMetadataResponse`
- `GetRechnungMetadata(GetRechnungMetadataRequest) -> GetRechnungMetadataResponse`

Port:

- Default `50051`
- Konfigurierbar beim Serverstart ueber erstes CLI-Argument
- Client-Defaults: `INVOICE_METADATA_HOST=localhost`, `INVOICE_METADATA_PORT=50051`

Status-/Fehler-Mapping:

- Fehlende Metadaten: `INVALID_ARGUMENT`, Beschreibung `metadata ist erforderlich`
- Fehlende Pflichtfelder: `INVALID_ARGUMENT`
- Dublette: `ALREADY_EXISTS`
- Leere ID beim Lesen: `INVALID_ARGUMENT`
- Nicht gefundene Rechnung: `NOT_FOUND`

### RabbitMQ

Queue:

- Default `payment.orders`
- Konfigurierbar per `PAYMENT_QUEUE`

RabbitMQ-Defaults:

- `RABBITMQ_HOST=localhost`
- `RABBITMQ_PORT=5672`
- `RABBITMQ_USERNAME=guest`
- `RABBITMQ_PASSWORD=guest`

Nachricht:

- JSON serialisierter `Zahlungsauftrag`
- `contentType=application/json`
- `deliveryMode=2`, also persistent markierte Message
- Queue wird durable deklariert

Consumer-Verhalten:

- `basicQos(1)`, also maximal eine unbestaetigte Nachricht gleichzeitig
- Bei Erfolg: `basicAck`
- Bei Fehler: `basicNack(..., requeue=false)`

## BPMN-relevante Prozesssicht

Ein sinnvolles Hauptmodell koennte heissen: `Eingangsrechnung verarbeiten`.

Moegliche Pools/Lanes:

- Pool `Rechnungsprozess / Camunda`
- Lane `Rechnungserfassung`
- Lane `Metadaten-Service`
- Lane `Zahlungsabwicklung`
- Externer Pool `RabbitMQ`
- Optional externer Pool `Zahlungsservice`

Moeglicher Happy Path:

1. Start Event: `Rechnung eingegangen`
2. Service Task: `Rechnungsmetadaten erfassen/uebernehmen`
3. Business Rule Task oder Service Task: `Pflichtfelder validieren`
4. Service Task: `Dublette pruefen`
5. Gateway: `Rechnung bereits erfasst?`
6. Falls nein: Service Task `Rechnungsmetadaten speichern`
7. Service Task: `Rechnungsmetadaten abrufen/bestaetigen`
8. Service Task: `Zahlungsauftrag erzeugen`
9. Send Task oder Service Task: `Zahlungsauftrag an Queue senden`
10. Intermediate Message Event oder separates Empfaenger-Modell: `Zahlungsauftrag empfangen`
11. Service Task: `Zahlung verarbeiten`
12. End Event: `Zahlung verarbeitet`

Moegliche Fehler-/Alternativpfade:

- Fehlende oder ungueltige Pflichtfelder: Boundary Error Event an Validierung, Ende `Rechnung abgelehnt / Korrektur erforderlich`
- Dublette erkannt: Gateway-Pfad zu Ende `Rechnung bereits erfasst`
- Metadatenservice nicht erreichbar: technischer Fehlerpfad mit Retry oder Incident
- Rechnung nach Speicherung nicht lesbar: Fehlerpfad `Inkonsistenz pruefen`
- RabbitMQ nicht erreichbar: Retry/Incident oder Kompensationspfad
- Zahlungsauftrag nicht verarbeitbar: Fehlerpfad `Zahlung fehlgeschlagen`, aktuell ohne Retry, weil `requeue=false`

## Wichtige Designfrage fuer Camunda

Der aktuelle Code hat noch keinen echten fachlichen Prozesszustand. Fuer Camunda muss entschieden werden, ob Camunda:

- nur den bestehenden technischen Ablauf orchestriert, oder
- der fuehrende Prozessmanager wird, der Status, Fehler, Wiederholungen und manuelle Entscheidungen steuert.

Empfehlung fuer ein BPMN-Modell:

- Camunda sollte die fachliche Prozessinstanz je Rechnung fuehren.
- Die `rechnungs_id` sollte Business Key oder Prozessvariable sein.
- Die Kombination `lieferanten_name + rechnungs_nummer` ist fachlich wichtig fuer Dubletten.
- Zahlungsauftrag sollte nach erfolgreicher Speicherung als eigener Schritt modelliert werden.
- RabbitMQ-Versand kann als Send Task/Service Task modelliert werden.
- Der Zahlungsservice kann als separates BPMN-Modell mit Message Start Event `Zahlungsauftrag erhalten` oder als externer technischer Worker dargestellt werden.

## Moegliche Prozessvariablen

- `rechnungsId`
- `lieferantenName`
- `rechnungsNummer`
- `rechnungsDatum`
- `gesamtbetragBrutto`
- `waehrung`
- `zahlungsId`
- `fachschluessel`
- `validierungErfolgreich`
- `dubletteGefunden`
- `zahlungVerarbeitet`
- `fehlerCode`
- `fehlerNachricht`

## Offene fachliche Fragen fuer die BPMN-Modellierung

- Kommt eine Rechnung spaeter aus Upload/OCR/E-Mail/API oder bleibt der Demo-Client der Starter?
- Soll bei ungueltigen Pflichtfeldern ein manueller Korrekturschritt entstehen?
- Soll bei Dubletten nur beendet werden oder soll ein Mensch entscheiden, ob es wirklich eine Dublette ist?
- Gibt es Freigaberegeln nach Betrag, Lieferant oder Kostenstelle?
- Soll eine Rechnung vor Zahlung sachlich/fachlich geprueft werden?
- Soll der Zahlungsauftrag synchron im Prozess erstellt werden oder asynchron ueber RabbitMQ?
- Soll der Zahlungsservice selbst durch Camunda gesteuert werden oder nur als externer Consumer laufen?
- Was soll bei Zahlungsfehlern passieren: Retry, Dead Letter Queue, manueller Task oder Prozessabbruch?
- Wird spaeter eine echte Datenbank statt In-Memory-Repository genutzt?
- Wird spaeter ein ERP-, Banking- oder Buchhaltungssystem angebunden?

## Kurzfassung fuer ChatGPT-Auftrag

Bitte modelliere ein BPMN-2.0-Prozessmodell fuer Camunda fuer die Eingangsrechnungsverarbeitung. Der bestehende Code speichert Rechnungsmetadaten per gRPC, validiert Pflichtfelder, prueft Dubletten anhand Lieferant plus Rechnungsnummer, erzeugt nach erfolgreicher Speicherung einen Zahlungsauftrag und sendet diesen als JSON ueber RabbitMQ an die Queue `payment.orders`. Ein Zahlungsservice konsumiert die Queue und bestaetigt erfolgreiche Verarbeitung. Fehlerfaelle sind ungueltige Daten, Dublette, nicht gefundene Rechnung, nicht erreichbarer Metadatenservice, nicht erreichbare Queue und nicht verarbeitbarer Zahlungsauftrag. Das Modell soll sowohl Happy Path als auch Fehlerpfade enthalten und sinnvolle Camunda-Service-Tasks, Gateways, Message Events und optionale manuelle Tasks vorschlagen.
