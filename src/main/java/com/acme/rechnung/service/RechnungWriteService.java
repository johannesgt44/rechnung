/* Gecoded mithilfe von KI, als vorlage SWA-Projekt genutzt, an dessen Stil orientiert.*/
package com.acme.rechnung.service;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.repository.RechnungRepository;

/** Geschäftslogik für schreibende Zugriffe auf Rechnungen bzw. Repository. */

public final class RechnungWriteService {
    /* Variable zum Herstellen einer Abhängigkeit zum Repo.*/
    private final RechnungRepository repository;

    /** Setzten der Beziehung/Abhängigkeit durch Constructor Injection.*/
    public RechnungWriteService(RechnungRepository repository) {
        this.repository = repository;
    }

    /** Service-Methode zum Erstellen eines Rechnungsobjektes mit Methodenaufruf zum Validieren und speichern
     * im Repository. */
    public Rechnungsdaten create(Rechnungsdaten metadata) {
        validierePflichtfelder(metadata);
        /* Überprüfung ob Rechnung aus der Request bereits existiert, im Fehlerfall eine Ausnahme werfen.*/
        repository.findRechnungsIdByFachschluessel(metadata.getLieferantenName(), metadata.getRechnungsNummer())
                .ifPresent(rechnungsId -> {
                    throw new RechnungBereitsErfasstException(
                            metadata.getLieferantenName(),
                            metadata.getRechnungsNummer()
                    );
                });
        return repository.create(metadata);
    }

    /* Validierungsmethode zum Überprüfen, ob alle Pflichtfelder zum Erstellen eines Objektes ausgefüllt wurden. */
    private static void validierePflichtfelder(Rechnungsdaten metadata) {
        if (metadata.getLieferantenName().isBlank() || metadata.getRechnungsNummer().isBlank()) {
            throw new IllegalArgumentException("lieferanten_name und rechnungs_nummer sind erforderlich");
        }
        if (metadata.getGesamtbetragBrutto().isBlank()) {
            throw new IllegalArgumentException("gesamtbetrag_brutto ist erforderlich");
        }
        if (metadata.getWaehrung().isBlank()) {
            throw new IllegalArgumentException("waehrung ist erforderlich");
        }
    }
}
