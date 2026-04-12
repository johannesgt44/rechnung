package com.acme.rechnung.metadata;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryRechnungMetadataRepository implements RechnungMetadataRepository {
    private final Map<String, Rechnungsdaten> rechnungen = new ConcurrentHashMap<>();

    @Override
    public Rechnungsdaten save(Rechnungsdaten metadata) {
        String RechnungId = metadata.getRechnungsId().isBlank()
                ? UUID.randomUUID().toString()
                : metadata.getRechnungsId();

        Rechnungsdaten gespeicherteMetadaten = metadata.toBuilder()
                .setRechnungsId(RechnungId)
                .build();

        rechnungen.put(RechnungId, gespeicherteMetadaten);
        return gespeicherteMetadaten;
    }
}
