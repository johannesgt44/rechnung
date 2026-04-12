package com.acme.rechnung.payment;

import com.acme.rechnung.invoice.v1.InvoiceMetadata;
import java.util.UUID;

// Objekt um eine Payment Order zu speichern
// TODO evt Typen anpassen
public record PaymentOrderv2(
        String paymentId,
        String invoiceId,
        String supplierName,
        String invoiceNumber,
        String amount,
        String currency
) {
    public static PaymentOrderv2 forInvoice(InvoiceMetadata metadata) {
        return new PaymentOrderv2(
                UUID.randomUUID().toString(),
                metadata.getInvoiceId(),
                metadata.getSupplierName(),
                metadata.getInvoiceNumber(),
                metadata.getGrossAmount(),
                metadata.getCurrency()
        );
    }
}
