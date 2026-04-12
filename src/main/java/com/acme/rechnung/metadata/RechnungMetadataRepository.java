package com.acme.rechnung.metadata;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;

interface RechnungMetadataRepository {
    Rechnungsdaten save(Rechnungsdaten metadata);
}
