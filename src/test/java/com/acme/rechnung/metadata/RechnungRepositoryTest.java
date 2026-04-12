package com.acme.rechnung.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.repository.RechnungRepository;
import org.junit.jupiter.api.Test;

class RechnungRepositoryTest {
    private final RechnungRepository repository = new RechnungRepository();

    @Test
    void createVergibtRechnungsIdWennKeineGesetztIst() {
        Rechnungsdaten angelegteRechnung = repository.create(beispielRechnung().build());

        assertTrue(!angelegteRechnung.getRechnungsId().isBlank());
    }

    @Test
    void findByIdLiefertGespeicherteRechnung() {
        Rechnungsdaten gespeicherteRechnung = repository.create(beispielRechnung().build());

        assertEquals(
                gespeicherteRechnung,
                repository.findById(gespeicherteRechnung.getRechnungsId()).orElseThrow()
        );
    }

    @Test
    void updateAktualisiertVorhandeneRechnung() {
        Rechnungsdaten gespeicherteRechnung = repository.create(beispielRechnung().build());
        Rechnungsdaten aktualisierteRechnung = gespeicherteRechnung.toBuilder()
                .setGesamtbetragBrutto("1290.00")
                .build();

        Rechnungsdaten ergebnis = repository.update(aktualisierteRechnung);

        assertEquals("1290.00", ergebnis.getGesamtbetragBrutto());
    }

    @Test
    void findRechnungsIdByFachschluesselLiefertTreffer() {
        Rechnungsdaten gespeicherteRechnung = repository.create(beispielRechnung().build());

        assertEquals(
                gespeicherteRechnung.getRechnungsId(),
                repository.findRechnungsIdByFachschluessel("Muster Lieferant GmbH", "RE-2026-0001").orElseThrow()
        );
    }

    private static Rechnungsdaten.Builder beispielRechnung() {
        return Rechnungsdaten.newBuilder()
                .setLieferantenName("Muster Lieferant GmbH")
                .setRechnungsNummer("RE-2026-0001")
                .setRechnungsDatum("2026-04-07")
                .setGesamtbetragBrutto("1190.00")
                .setWaehrung("EUR");
    }
}
