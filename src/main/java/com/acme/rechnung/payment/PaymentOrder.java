package com.acme.rechnung.payment;

import com.acme.rechnung.invoice.v1.InvoiceMetadata;
import java.util.UUID;

// Objekt um eine Payment Order zu speichern
// TODO evt Typen anpassen
public record PaymentOrder(
        String paymentId,
        String invoiceId,
        String supplierName,
        String invoiceNumber,
        String amount,
        String currency
) {
    public static PaymentOrder forInvoice(InvoiceMetadata metadata) {
        return new PaymentOrder(
                UUID.randomUUID().toString(),
                metadata.getInvoiceId(),
                metadata.getSupplierName(),
                metadata.getInvoiceNumber(),
                metadata.getGrossAmount(),
                metadata.getCurrency()
        );
    }
}
