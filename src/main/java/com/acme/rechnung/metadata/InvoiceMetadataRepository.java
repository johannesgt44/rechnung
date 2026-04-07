package com.acme.rechnung.metadata;

import com.acme.rechnung.invoice.v1.InvoiceMetadata;

interface InvoiceMetadataRepository {
    InvoiceMetadata save(InvoiceMetadata metadata);
}
