package com.acme.rechnung.metadata;

import com.acme.rechnung.invoice.v1.InvoiceMetadata;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryInvoiceMetadataRepository implements InvoiceMetadataRepository {
    private final Map<String, InvoiceMetadata> invoices = new ConcurrentHashMap<>();

    @Override
    public InvoiceMetadata save(InvoiceMetadata metadata) {
        String invoiceId = metadata.getInvoiceId().isBlank()
                ? UUID.randomUUID().toString()
                : metadata.getInvoiceId();

        InvoiceMetadata storedMetadata = metadata.toBuilder()
                .setInvoiceId(invoiceId)
                .build();

        invoices.put(invoiceId, storedMetadata);
        return storedMetadata;
    }
}
