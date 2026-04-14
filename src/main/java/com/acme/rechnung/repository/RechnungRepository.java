/*Gecoded mithilfe von KI, als Vorlage SWA-Projekt genutzt, an dessen Stil orientiert.*/
package com.acme.rechnung.repository;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Ein einfacher Speicher für die Rechnungsmetadaten

public final class RechnungRepository {
    private final Map<String, Rechnungsdaten> rechnungen = new ConcurrentHashMap<>();
    private final Map<String, String> rechnungIdsNachFachschluessel = new ConcurrentHashMap<>();

    // speichert die Rechnung Lokal
    public Rechnungsdaten create(Rechnungsdaten metadata) {
        String rechnungsId = metadata.getRechnungsId().isBlank()
                ? UUID.randomUUID().toString()
                : metadata.getRechnungsId();
        Rechnungsdaten gespeicherteMetadaten = metadata.toBuilder()
                .setRechnungsId(rechnungsId)
                .build();
        rechnungen.put(rechnungsId, gespeicherteMetadaten);
        rechnungIdsNachFachschluessel.put(fachschluesselVon(gespeicherteMetadaten), rechnungsId);
        return gespeicherteMetadaten;
    }

    // Sucht Rechnung nach einer ID
    // Optioanal damit es bei nicht gefunden die Exceptiion wirft
    // Mehtode wurde durch KI geschrieben
    public Optional<Rechnungsdaten> findById(String rechnungsId) {
        return Optional.ofNullable(rechnungen.get(rechnungsId));
    }

    // Sucht nicht die ganze Rechnung sondern die rechnungsId über Den Fachschlüssel
    // Optioanal damit es bei nicht gefunden die Exceptiion wirft
    // Idee mit dem Fachschlüssel ist von KI um Duppletten zu vermeiden
    public Optional<String> findRechnungsIdByFachschluessel(String lieferantenName, String rechnungsNummer) {
        return Optional.ofNullable(
                rechnungIdsNachFachschluessel.get(fachschluesselVon(lieferantenName, rechnungsNummer))
        );
    }

    // Liest Lieferantenname und Rechnungsnummer aus den Rechnungsdaten aus
    // und übergibt sie an die eigentliche Fachschlüssel-Bildung
    // KI
    private static String fachschluesselVon(Rechnungsdaten metadata) {
        return fachschluesselVon(metadata.getLieferantenName(), metadata.getRechnungsNummer());
    }
    // Normalisiert Lieferantenname und Rechnungsnummer und kombiniert beide
    // zu einem eindeutigen fachlichen Schlüssel
    // KI
    private static String fachschluesselVon(String lieferantenName, String rechnungsNummer) {
        return lieferantenName.trim().toLowerCase() + "::" + rechnungsNummer.trim().toLowerCase();
    }
}
