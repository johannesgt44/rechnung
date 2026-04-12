package com.acme.rechnung.metadata;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;

// Interface für den Zugriff auf Rechnungsmetadaten.

interface RechnungMetadataRepository {
    Rechnungsdaten save(Rechnungsdaten metadata);
}
