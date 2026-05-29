/* Gecoded mithilfe von KI, als vorlage SWA-Projekt genutzt, an dessen Stil orientiert.*/
package com.acme.rechnung.service;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.repository.RechnungRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Geschäftslogik für schreibende Zugriffe auf Rechnungen bzw. Repository. */

public final class RechnungWriteService {
    /* Variable zum Herstellen einer Abhängigkeit zum Repo.*/
    private final RechnungRepository repository;

    /** Setzten der Beziehung/Abhängigkeit durch Constructor Injection.*/
    public RechnungWriteService(RechnungRepository repository) {
        this.repository = repository;
    }

    /** Service-Methode zum Erstellen eines Rechnungsobjektes mit Methodenaufruf zum Validieren und speichern
     * im Repository. */
    public Rechnungsdaten create(Rechnungsdaten metadata) {
        validierePflichtfelder(metadata);
        /* Überprüfung ob Rechnung aus der Request bereits existiert, im Fehlerfall eine Ausnahme werfen.*/
        if (repository.existsByRechnungsNummer(metadata.getRechnungsNummer())) {
            throw new RechnungBereitsErfasstException(
                    metadata.getLieferantenName(),
                    metadata.getRechnungsNummer()
            );
        }
        return repository.create(metadata);
    }

    /* Validierungsmethode zum Überprüfen, ob alle Pflichtfelder zum Erstellen eines Objektes ausgefüllt wurden. */
    private static void validierePflichtfelder(Rechnungsdaten metadata) {
        if (metadata.getLieferantenName().isBlank() || metadata.getRechnungsNummer().isBlank()) {
            throw new IllegalArgumentException("lieferanten_name und rechnungs_nummer sind erforderlich");
        }
        if (!metadata.getRechnungsNummer().matches("\\d+")) {
            throw new IllegalArgumentException("rechnungs_nummer darf nur Ziffern enthalten");
        }
        if (metadata.getGesamtbetragBrutto().isBlank()) {
            throw new IllegalArgumentException("gesamtbetrag_brutto ist erforderlich");
        }
        double gesamtbetragBrutto = validiereZahl(metadata.getGesamtbetragBrutto(), "gesamtbetrag_brutto");
        if (gesamtbetragBrutto <= 0) {
            throw new IllegalArgumentException("gesamtbetrag_brutto muss groesser als 0 sein");
        }
        if (metadata.getWaehrung().isBlank()) {
            throw new IllegalArgumentException("waehrung ist erforderlich");
        }
        validiereRechnungsDatum(metadata.getRechnungsDatum());
        if (metadata.getZahlungsziel().isBlank()) {
            throw new IllegalArgumentException("zahlungsziel ist erforderlich");
        }
        if (metadata.getRechnungspostenCount() == 0) {
            throw new IllegalArgumentException("mindestens ein rechnungsposten ist erforderlich");
        }
        double postenSummeBrutto = 0;
        for (var rechnungsposten : metadata.getRechnungspostenList()) {
            String einheit = rechnungsposten.getEinheit();
            if (!einheit.equals("Stk.") && !einheit.equals("Std.") && !einheit.equals("Pauschal")) {
                throw new IllegalArgumentException("einheit muss Stk., Std. oder Pauschal sein");
            }
            double menge = validiereZahl(rechnungsposten.getMenge(), "menge");
            if (menge <= 0) {
                throw new IllegalArgumentException("menge muss groesser als 0 sein");
            }
            double einzelpreisBrutto = validiereZahl(rechnungsposten.getEinzelpreisBrutto(), "einzelpreis_brutto");
            if (einzelpreisBrutto < 0) {
                throw new IllegalArgumentException("einzelpreis_brutto muss groesser oder gleich 0 sein");
            }
            validiereZahl(rechnungsposten.getSteuerProzent(), "steuer_prozent");
            postenSummeBrutto += menge * einzelpreisBrutto;
        }
        if (Math.abs(postenSummeBrutto - gesamtbetragBrutto) > 0.01) {
            throw new IllegalArgumentException("summe der rechnungsposten muss zum gesamtbetrag_brutto passen");
        }
    }

    private static void validiereRechnungsDatum(String rechnungsDatum) {
        if (rechnungsDatum.isBlank()) {
            return;
        }
        try {
            LocalDate datum = LocalDate.parse(rechnungsDatum);
            if (datum.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("rechnungs_datum darf nicht in der Zukunft liegen");
            }
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("rechnungs_datum muss im Format JJJJ-MM-TT sein");
        }
    }

    private static double validiereZahl(String value, String feldname) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(feldname + " ist erforderlich");
        }
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(feldname + " muss eine Zahl sein");
        }
    }
}
