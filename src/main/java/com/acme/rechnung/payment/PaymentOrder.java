package com.acme.rechnung.payment;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
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
    public static PaymentOrder forInvoice(Rechnungsdaten metadata) {
        return new PaymentOrder(
                UUID.randomUUID().toString(),
                metadata.getRechnungsId(),
                metadata.getLieferantenName(),
                metadata.getRechnungsNummer(),
                metadata.getGesamtbetragBrutto(),
                metadata.getWaehrung()
        );
    }
}
