package com.acme.rechnung.service;

public final class RechnungBereitsErfasstException extends RuntimeException {
    public RechnungBereitsErfasstException(String lieferantenName, String rechnungsNummer) {
        super("Die Rechnung von '" + lieferantenName + "' mit der Nummer '" + rechnungsNummer
                + "' wurde bereits erfasst.");
    }

    public static RechnungBereitsErfasstException fuerRechnungsId(String rechnungsId) {
        return new RechnungBereitsErfasstException(
                "Die Rechnung mit der ID '" + rechnungsId + "' wurde bereits erfasst."
        );
    }

    private RechnungBereitsErfasstException(String nachricht) {
        super(nachricht);
    }
}
