package com.acme.rechnung.service;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.repository.RechnungRepository;

// Geschäftslogik für lesende Zugriffe auf Rechnungen.

public final class RechnungService {
    private final RechnungRepository repository;

    public RechnungService(RechnungRepository repository) {
        this.repository = repository;
    }

    public Rechnungsdaten findById(String rechnungsId) {
        return repository.findById(rechnungsId)
                .orElseThrow(() -> new RechnungNichtGefundenException("Keine Rechnung mit dieser ID gefunden."));
    }
}
