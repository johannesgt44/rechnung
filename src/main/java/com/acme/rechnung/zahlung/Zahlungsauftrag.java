package com.acme.rechnung.zahlung;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import java.util.UUID;

/// Objekt, um einen Zahlungsauftrag zu speichern.
public record Zahlungsauftrag(
        String zahlungsId,
        String rechnungsId,
        String lieferantenName,
        String rechnungsNummer,
        String betrag,
        String waehrung
) {
    public static Zahlungsauftrag toZahlungsauftrag(Rechnungsdaten rechnungsdaten) {
        return new Zahlungsauftrag(
                UUID.randomUUID().toString(),
                rechnungsdaten.getRechnungsId(),
                rechnungsdaten.getLieferantenName(),
                rechnungsdaten.getRechnungsNummer(),
                rechnungsdaten.getGesamtbetragBrutto(),
                rechnungsdaten.getWaehrung()
        );
    }
}
