package com.acme.rechnung.service;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.repository.RechnungRepository;

// Geschäftslogik für schreibende Zugriffe auf Rechnungen.

public final class RechnungWriteService {
    private final RechnungRepository repository;

    public RechnungWriteService(RechnungRepository repository) {
        this.repository = repository;
    }

    public Rechnungsdaten create(Rechnungsdaten metadata) {
        validierePflichtfelder(metadata, false);
        repository.findRechnungsIdByFachschluessel(metadata.getLieferantenName(), metadata.getRechnungsNummer())
                .ifPresent(rechnungsId -> {
                    throw new RechnungBereitsErfasstException(
                            metadata.getLieferantenName(),
                            metadata.getRechnungsNummer()
                    );
                });
        return repository.create(metadata);
    }

    public Rechnungsdaten update(Rechnungsdaten metadata) {
        validierePflichtfelder(metadata, true);
        Rechnungsdaten bestehendeRechnung = repository.findById(metadata.getRechnungsId())
                .orElseThrow(() -> new RechnungNichtGefundenException(metadata.getRechnungsId()));

        repository.findRechnungsIdByFachschluessel(metadata.getLieferantenName(), metadata.getRechnungsNummer())
                .ifPresent(vorhandeneRechnungsId -> {
                    if (!vorhandeneRechnungsId.equals(metadata.getRechnungsId())) {
                        throw new RechnungBereitsErfasstException(
                                metadata.getLieferantenName(),
                                metadata.getRechnungsNummer()
                        );
                    }
                });

        return repository.update(metadata);
    }

    private static void validierePflichtfelder(Rechnungsdaten metadata, boolean update) {
        if (update && metadata.getRechnungsId().isBlank()) {
            throw new IllegalArgumentException("rechnungs_id ist für Updates erforderlich");
        }
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
