package com.acme.rechnung.service;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Validiert Rechnungsdaten vor dem Speichern. */
public final class RechnungValidator {
    public void validate(Rechnungsdaten metadata) {
        if (metadata.getLieferantenName().isBlank() || metadata.getRechnungsNummer().isBlank()) {
            throw new IllegalArgumentException("Lieferantenname und Rechnungsnummer sind erforderlich.");
        }
        if (!metadata.getRechnungsNummer().matches("\\d+")) {
            throw new IllegalArgumentException("Die Rechnungsnummer darf nur Ziffern enthalten.");
        }
        if (metadata.getGesamtbetragBrutto().isBlank()) {
            throw new IllegalArgumentException("Der Gesamtbetrag brutto ist erforderlich.");
        }
        double gesamtbetragBrutto = validiereZahl(metadata.getGesamtbetragBrutto(), "Der Gesamtbetrag brutto");
        if (gesamtbetragBrutto <= 0) {
            throw new IllegalArgumentException("Der Gesamtbetrag brutto muss groesser als 0 sein.");
        }
        if (metadata.getWaehrung().isBlank()) {
            throw new IllegalArgumentException("Die Waehrung ist erforderlich.");
        }
        validiereRechnungsDatum(metadata.getRechnungsDatum());
        if (metadata.getZahlungsziel().isBlank()) {
            throw new IllegalArgumentException("Das Zahlungsziel ist erforderlich.");
        }
        if (metadata.getRechnungspostenCount() == 0) {
            throw new IllegalArgumentException("Mindestens ein Rechnungsposten ist erforderlich.");
        }

        double postenSummeBrutto = 0;
        for (var rechnungsposten : metadata.getRechnungspostenList()) {
            String einheit = rechnungsposten.getEinheit();
            if (!einheit.equals("Stk.") && !einheit.equals("Std.") && !einheit.equals("Pauschal")) {
                throw new IllegalArgumentException("Die Einheit muss Stk., Std. oder Pauschal sein.");
            }
            double menge = validiereZahl(rechnungsposten.getMenge(), "Die Menge");
            if (menge <= 0) {
                throw new IllegalArgumentException("Die Menge muss groesser als 0 sein.");
            }
            double einzelpreisBrutto = validiereZahl(rechnungsposten.getEinzelpreisBrutto(), "Der Einzelpreis brutto");
            if (einzelpreisBrutto < 0) {
                throw new IllegalArgumentException("Der Einzelpreis brutto muss groesser oder gleich 0 sein.");
            }
            validiereZahl(rechnungsposten.getSteuerProzent(), "Der Steuersatz");
            postenSummeBrutto += menge * einzelpreisBrutto;
        }

        if (Math.abs(postenSummeBrutto - gesamtbetragBrutto) > 0.01) {
            throw new IllegalArgumentException("Die Summe der Rechnungsposten muss zum Gesamtbetrag brutto passen.");
        }
    }

    private static void validiereRechnungsDatum(String rechnungsDatum) {
        if (rechnungsDatum.isBlank()) {
            return;
        }
        try {
            LocalDate datum = LocalDate.parse(rechnungsDatum);
            if (datum.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Das Rechnungsdatum darf nicht in der Zukunft liegen.");
            }
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Das Rechnungsdatum muss im Format JJJJ-MM-TT sein.");
        }
    }

    private static double validiereZahl(String value, String feldname) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(feldname + " ist erforderlich.");
        }
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(feldname + " muss eine Zahl sein.");
        }
    }
}
