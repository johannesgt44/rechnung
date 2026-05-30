/* Gecoded mithilfe von KI, als vorlage SWA-Projekt genutzt, an dessen Stil orientiert.*/
package com.acme.rechnung.service;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.repository.RechnungRepository;

/** Geschäftslogik für schreibende Zugriffe auf Rechnungen bzw. Repository. */
public final class RechnungWriteService {
    /* Variable zum Herstellen einer Abhängigkeit zum Repo.*/
    private final RechnungRepository repository;
    private final RechnungValidator rechnungValidator;

    /** Setzten der Beziehung/Abhängigkeit durch Constructor Injection.*/
    public RechnungWriteService(RechnungRepository repository) {
        this.repository = repository;
        this.rechnungValidator = new RechnungValidator();
    }

    /** Service-Methode zum Erstellen eines Rechnungsobjektes mit Methodenaufruf zum Validieren und speichern
     * im Repository. */
    public Rechnungsdaten create(Rechnungsdaten metadata) {
        rechnungValidator.validate(metadata);
        /* Überprüfung ob Rechnung aus der Request bereits existiert, im Fehlerfall eine Ausnahme werfen.*/
        if (repository.existsByRechnungsNummer(metadata.getRechnungsNummer())) {
            throw new RechnungBereitsErfasstException(
                    metadata.getLieferantenName(),
                    metadata.getRechnungsNummer()
            );
        }
        return repository.create(metadata);
    }
}
