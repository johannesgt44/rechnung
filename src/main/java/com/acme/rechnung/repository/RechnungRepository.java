/*Gecoded mithilfe von KI, als Vorlage SWA-Projekt genutzt, an dessen Stil orientiert.*/
package com.acme.rechnung.repository;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Ein einfacher Speicher für die Rechnungsmetadaten

public final class RechnungRepository {
    private final Map<String, Rechnungsdaten> rechnungen = new ConcurrentHashMap<>();

    // speichert die Rechnung Lokal
    public Rechnungsdaten create(Rechnungsdaten metadata) {
        String rechnungsNummer = metadata.getRechnungsNummer();
        rechnungen.put(rechnungsNummer, metadata);
        return metadata;
    }

    // Sucht Rechnung nach ihrer Rechnungsnummer
    // Optioanal damit es bei nicht gefunden die Exceptiion wirft
    // Mehtode wurde durch KI geschrieben
    public Optional<Rechnungsdaten> findById(String rechnungsNummer) {
        return Optional.ofNullable(rechnungen.get(rechnungsNummer));
    }

    // Prüft, ob eine Rechnung mit dieser Rechnungsnummer bereits gespeichert ist.
    public boolean existsByRechnungsNummer(String rechnungsNummer) {
        return rechnungen.containsKey(rechnungsNummer);
    }
}
