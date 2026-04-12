package com.acme.rechnung.payment;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import java.util.UUID;

// Objekt um eine Payment Order zu speichern
// TODO evt Typen anpassen
public record Zahlungsauftrag(
        String zahlungsId,
        String rechungsId,
        String LiefernatenName,
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
