# Eingangsrechnungsverarbeitung mit Camunda und gRPC

Dieses Projekt ist Teil des DvG-Projekts zur Digitalisierung eines Eingangsrechnungsprozesses. Ziel ist nicht mehr, dass ein lokaler Client den kompletten Ablauf steuert, sondern dass **Camunda 8 Cloud den Prozess orchestriert**. Die einzelnen technischen Komponenten werden über Service Tasks und Job Worker angebunden.

Dieses Repository `rechnung` enthält den Rechnungsmetadaten-Service und den Camunda Worker für den Service Task `Rechnungsmetadaten speichern`. Nach erfolgreicher Rechnungsspeicherung kann ein separater Zahlungsservice im Projekt `../Zahlungssystem` angebunden werden; dieser wird in einer eigenen README dokumentiert.

## Architekturidee

Camunda ist der führende Prozess-Orchestrator. Das bedeutet:

- Camunda entscheidet, welcher Prozessschritt als nächstes ausgeführt wird.
- Jeder Service Task hat einen eigenen `Job type`.
- Ein passender Worker hört auf diesen Job Type.
- Der Worker führt genau eine technische Aufgabe aus und meldet das Ergebnis an Camunda zurück.

## Komponenten im Rechnungsprojekt

```text
src/main/java/com/acme/rechnung
+-- camunda
|   +-- BaseCamundaWorker.java
|   +-- CamundaErrorHandler.java
|   +-- JobInformation.java
|   +-- RechnungsmetadatenSpeichernWorker.java
+-- metadata
|   +-- RechnungMetadataGrpcService.java
|   +-- RechnungMetadataServer.java
+-- repository
|   +-- RechnungRepository.java
+-- service
    +-- RechnungBereitsErfasstException.java
    +-- RechnungNichtGefundenException.java
    +-- RechnungService.java
    +-- RechnungValidator.java
    +-- RechnungWriteService.java
```

### gRPC-Server

Der Server wird über `RechnungMetadataServer` gestartet und lauscht standardmäßig auf Port `50051`.

```powershell
.\gradlew.bat run
```

Der gRPC-Vertrag liegt in:

```text
src/main/proto/rechnung_metadata.proto
```

Der Service stellt zwei Operationen bereit:

```text
CreateRechnungMetadata
GetRechnungMetadata
```

### Warum gRPC und Proto?

gRPC wurde im Projekt als technische Schnittstelle vorgegeben und verbindet den Camunda Worker mit dem Rechnungsservice. Die `.proto`-Datei beschreibt dabei den gemeinsamen Vertrag.

Vorteile:

- Die Schnittstelle ist klar typisiert.
- Aus der `.proto`-Datei werden passende Java-Klassen generiert.
- Worker und Service haben denselben Datenvertrag.
- Camunda orchestriert einen externen Service, statt direkt Repository-Code auszuführen.

## Camunda Worker

Der Worker für Rechnungsmetadaten wird über diesen Task gestartet:

```powershell
.\gradlew.bat runRechnungsmetadatenWorker
```

Er verbindet sich mit Camunda Cloud und wartet auf Jobs vom Typ:

```text
rechnung-metadaten-speichern
```

Dieser Job Type muss im Camunda Modeler beim Service Task `Rechnungsmetadaten speichern` eingetragen werden.

Der Worker orientiert sich am Pattern aus:

```text
https://github.com/camunda-community-hub/C7-C8-workers
```

Die Grundidee daraus:

- `BaseCamundaWorker` kapselt die gemeinsamen Camunda-Funktionen.
- Jeder konkrete Worker implementiert `getType()` und `executeWorker(...)`.
- `JobInformation` kapselt den Zugriff auf Prozessvariablen.
- Jobs werden über `complete(...)`, `fail(...)` oder `throwBpmnError(...)` beendet.

## Prozessvariablen für Camunda

Diese Variablen sollte Camunda an den Service Task übergeben:

```json
{
  "lieferantenName": "Muster GmbH",
  "lieferantenNummer": "L-1001",
  "rechnungsNummer": "2026001",
  "rechnungsDatum": "2026-04-02",
  "zahlungsziel": "14 Tage netto",
  "bemerkungen": "optional",
  "gesamtbetragBrutto": "119.00",
  "waehrung": "EUR",
  "rechnungsposten": [
    {
      "position": 1,
      "beschreibung": "Beratungsleistung",
      "menge": "1",
      "einheit": "Std.",
      "einzelpreisBrutto": "119.00",
      "steuerProzent": "19"
    }
  ]
}
```

Alternativ kann Camunda für das Zahlungsziel auch eine Zahl liefern:

```json
{
  "zahlungszielTage": 14
}
```

Der Worker macht daraus:

```text
14 Tage netto
```

### Rechnungsposten in Camunda Forms

Rechnungsposten sind eine Liste. In Camunda Forms sollte dafür eine **Dynamic List** verwendet werden.

Empfohlener Path:

```text
rechnungsposten
```

Felder innerhalb der Dynamic List:

```text
position
beschreibung
menge
einheit
einzelpreisBrutto
steuerProzent
```

Zulässige Werte für `einheit`:

```text
Stk.
Std.
Pauschal
```

Der Gesamtbetrag brutto und die einzelnen Bruttowerte werden als Strings übertragen, damit Werte wie `119.00` sauber erhalten bleiben.

## Validierung

Die Validierung wurde bewusst aus dem `RechnungWriteService` in eine eigene Klasse ausgelagert:

```text
RechnungValidator
```

Warum?

- Die Validierung ist fachliche Logik.
- Der Schreibservice soll nicht mit zu vielen Details überladen werden.
- Fehlermeldungen können zentral gepflegt werden.
- Die Meldungen werden später in Camunda Forms angezeigt.

Aktuelle Validierungsregeln:

- Lieferantenname ist erforderlich.
- Rechnungsnummer ist erforderlich.
- Rechnungsnummer darf nur Ziffern enthalten.
- Gesamtbetrag brutto ist erforderlich.
- Gesamtbetrag brutto muss größer als 0 sein.
- Währung ist erforderlich.
- Rechnungsdatum darf nicht in der Zukunft liegen.
- Rechnungsdatum wird im Format `JJJJ-MM-TT` erwartet.
- Zahlungsziel ist erforderlich.
- Mindestens ein Rechnungsposten ist erforderlich.
- Einheit muss `Stk.`, `Std.` oder `Pauschal` sein.
- Menge je Rechnungsposten muss größer als 0 sein.
- Einzelpreis brutto je Rechnungsposten muss größer oder gleich 0 sein.
- Steuersatz muss eine Zahl sein.
- Summe der Rechnungsposten muss ungefähr zum Gesamtbetrag brutto passen.

## Rechnungsnummer als Schlüssel

Früher gab es gedanklich sowohl eine `rechnungsId` als auch eine `rechnungsNummer`. Das wurde vereinfacht.

Aktuell gilt:

```text
Die Rechnungsnummer ist der fachliche Schlüssel.
```

Das Repository speichert Rechnungen anhand der `rechnungsNummer`. Beim Speichern wird geprüft, ob diese Nummer bereits existiert. Wenn ja, entsteht ein fachlicher Fehler.

Warum so?

- Die ERP-Maske arbeitet sichtbar mit Rechnungsnummern.
- Dublettenprüfung wird einfacher erklärbar.

## Fachliche und technische Fehler

Eine der wichtigsten Entscheidungen ist die Trennung zwischen fachlichen und technischen Fehlern.

### Fachlicher Fehler

Ein fachlicher Fehler bedeutet:

```text
Der Prozess oder die eingegebenen Daten sind fachlich falsch.
```

Beispiele:

- Rechnung ist ungültig.
- Rechnungsnummer ist schon vorhanden.
- Rechnungsposten passen nicht zum Gesamtbetrag.

Diese Fehler werden als BPMN Error an Camunda gemeldet:

```text
RECHNUNG_UNGUELTIG
RECHNUNG_BEREITS_ERFASST
```

Camunda soll solche Fehler im BPMN-Modell fangen und fachlich weiterverarbeiten, z.B.:

- Zurück zur manuellen Korrektur
- kontrollierte Fortsetzung bei bereits erfasster Rechnung

### Technischer Fehler

Ein technischer Fehler bedeutet:

```text
Die Daten können in Ordnung sein, aber ein System ist nicht erreichbar oder kaputt.
```

Beispiele:

- gRPC-Rechnungsservice ist aus.
- Netzwerkproblem.
- Timeout.

Diese Fehler werden **nicht** als BPMN Error modelliert. Der Worker ruft `fail(...)` auf. Camunda reduziert die Retries und erzeugt nach Ablauf der Retries einen Incident.

Warum?

- Technische Fehler sind Betriebsprobleme.
- Ein Prozesspfad wie "Rechnung ungültig" wäre fachlich falsch.
- In echten Unternehmen werden solche Fälle über Retries, Monitoring und Incidents behandelt.

## Verhalten bei Fehlern in Camunda

### Ungültige Rechnung

Beispiel:

```text
Gesamtbetrag brutto = 100.00
Summe der Rechnungsposten = 119.00
```

Ablauf:

```text
Validator wirft IllegalArgumentException
gRPC-Service antwortet INVALID_ARGUMENT
Worker macht throwBpmnError("RECHNUNG_UNGUELTIG")
Camunda Boundary Error Event fängt den Fehler
Prozess geht zur Korrektur
```

### Rechnung bereits erfasst

Ablauf:

```text
Repository erkennt vorhandene Rechnungsnummer
gRPC-Service antwortet ALREADY_EXISTS
Worker macht throwBpmnError("RECHNUNG_BEREITS_ERFASST")
Camunda Boundary Error Event fängt den Fehler
Prozess kann mit der vorhandenen Rechnungsnummer weiterlaufen
```

### Worker aus

Wenn der Worker nicht läuft:

```text
Camunda legt den Job bereit
Retries bleiben unverändert
Prozess wartet am Service Task
```

Das ist normal. Camunda kann den Job niemandem geben, also gibt es auch noch keinen technischen Fail.

### Worker an, Rechnungsservice aus

Wenn der Worker läuft, aber der gRPC-Service nicht:

```text
Worker bekommt Job
Worker ruft localhost:50051 auf
gRPC meldet UNAVAILABLE
CamundaErrorHandler erkennt technischen Fehler
Worker macht fail(...)
Camunda reduziert Retries
Nach 0 Retries entsteht ein Incident
```

Das ist der Demo-Fall für technische Fehler.

### Nach 3 fehlgeschlagenen Retries

Nach den Retries entsteht in Camunda ein technischer Fehler am Service Task. Der Prozess wird an dieser Stelle nicht fortgesetzt.

## Boundary Error Events im BPMN-Modell

Am Service Task `Rechnungsmetadaten speichern` hängen zwei Boundary Error Events.

### Fehlercode für ungültige Rechnung

```text
RECHNUNG_UNGUELTIG
```

Möglicher Pfad:

```text
Rechnungsdaten korrigieren
```

### Fehlercode für Dublette

```text
RECHNUNG_BEREITS_ERFASST
```

Möglicher Pfad:

```text
Prozess mit vorhandener Rechnungsnummer fortsetzen
```

Technische Fehler bekommen kein Boundary Error Event. Sie laufen über Retries und Incidents.

## Fehlermeldung in Camunda Forms anzeigen

Der Worker setzt beim BPMN Error zusätzlich Prozessvariablen:

```text
bpmnErrorCode
validierungsFehler
validierungsfehler
```

In einer Korrektur-Form kann ein Readonly-Textfeld diesen Key verwenden:

```text
validierungsfehler
```

Dann sieht der Nutzer z.B.:

```text
Die Summe der Rechnungsposten muss zum Gesamtbetrag brutto passen.
```

Falls im Boundary Event ein Output Mapping gesetzt werden soll:

```text
Process variable name: validierungsfehler
Variable assignment value: =validierungsfehler
```

## Demo starten

### 1. Rechnungsservice starten

Im Projekt `rechnung`:

```powershell
.\gradlew.bat run
```

Erwartete Ausgabe:

```text
InvoiceMetadataService started on port 50051
```

### 2. Rechnungsmetadaten-Worker starten

Im Projekt `rechnung`:

```powershell
.\gradlew.bat runRechnungsmetadatenWorker
```

Erwartete Ausgabe:

```text
Teste Camunda Cloud Verbindung...
Camunda Cloud Verbindung OK.
Job Worker gestartet und wartet auf Jobs vom Typ: rechnung-metadaten-speichern
```

### 3. Prozess in Camunda starten

Im Camunda Modeler/Tasklist:

1. Rechnung manuell erfassen.
2. Rechnungsposten über Dynamic List eintragen.
3. Prozess bis `Rechnungsmetadaten speichern` laufen lassen.
4. Danach kann der Prozess mit Compliance, Freigabe oder Zahlung fortgesetzt werden.

## Camunda Cloud Konfiguration

Die Datei:

```text
CamundaClientCredentials.properties
```

muss im Projektordner liegen. Sie enthält:

```properties
camunda.client.cloud.cluster-id=...
camunda.client.cloud.region=...
camunda.client.auth.client-id=...
camunda.client.auth.client-secret=...
```

Diese Datei enthält Secrets und gehört nicht ins Git-Repository.

## Wichtige Gradle-Tasks

Im Rechnungsprojekt:

```powershell
.\gradlew.bat run
.\gradlew.bat runRechnungsmetadatenWorker
.\gradlew.bat compileJava
.\gradlew.bat test
```

## Begründung wichtiger Entscheidungen

### Warum Camunda Worker statt direkter HTTP-Connector?

Im Modeler steht beim Service Task nur ein Job Type. Deshalb ist ein Worker passend: Camunda stellt Jobs bereit, der Worker fragt Jobs ab und führt die technische Arbeit aus.

### Warum kein zentraler Worker für alles?

Ein zentraler Worker würde wieder eine eigene Prozesssteuerung im Code bauen. Besser ist:

```text
Ein Service Task = ein Job Type = ein Worker
```

Damit bleibt Camunda die Orchestrierungsinstanz.

### Warum fachliche Fehler als BPMN Error?

Fachliche Fehler sind Teil des Prozesses. Ein Mensch oder ein Prozesspfad kann darauf reagieren:

- Korrektur
- Dublettenprüfung
- fachliche Ablehnung

### Warum technische Fehler als Retry/Incident?

Technische Fehler sind keine fachliche Entscheidung. Wenn ein Service aus ist, ist die Rechnung nicht automatisch falsch. Deshalb:

```text
fail()
-> Retry
-> Incident
-> technische Ursache beheben
-> Retry in Operate
```

### Warum Validierung getrennt?

Der `RechnungWriteService` soll speichern und Dubletten prüfen. Die Detailregeln liegen in `RechnungValidator`. Dadurch ist der Code nachvollziehbarer und die Fehlermeldungen sind an einer Stelle gepflegt.

### Warum Rechnungsnummer als Schlüssel?

Für den aktuellen Demo-Stand reicht die Rechnungsnummer als fachlicher Primärschlüssel. Eine separate technische ID würde den Prozess unnötig verkomplizieren.

### Warum Bruttowerte und Steuer, aber kein Netto speichern?

In der ERP-Maske werden Bruttowerte und Steuersatz erfasst. Netto und Steuerbetrag können daraus berechnet werden. Deshalb werden nicht alle berechenbaren Werte gespeichert.

## Kurzfassung für die Abgabe

Das Projekt zeigt eine orchestrierte Eingangsrechnungsverarbeitung mit Camunda 8 Cloud. Camunda steuert den Prozess über Service Tasks und Job Types. Der Rechnungsmetadaten-Worker ruft einen gRPC-Service auf, der Rechnungen validiert, Dubletten erkennt und in-memory speichert. Fachliche Fehler werden als BPMN Errors modelliert und können im Prozess über Boundary Error Events behandelt werden. Technische Fehler werden bewusst über Camunda Retries und Incidents behandelt. Nach erfolgreicher Rechnungsspeicherung kann der Prozess an weitere Schritte wie Compliance, Freigabe oder Zahlung übergeben werden; diese Folgekomponenten werden separat dokumentiert.

