/*Gecoded mithilfe von KI, als vorlage SWA-Projekt genutzt, an dessen Stil orientiert.*/
package com.acme.rechnung.service;

import com.acme.rechnung.invoice.v1.Rechnungsdaten;
import com.acme.rechnung.repository.RechnungRepository;

/** Geschäftslogik für lesende Zugriffe auf Rechnungen.*/
public final class RechnungService {
    /* Abhängigkeit zum Repository zum Methodenaufruf herstellen*/
    private final RechnungRepository repository;

    /** Constructor Injection, zum Setzen der Beziehung*/
    public RechnungService(RechnungRepository repository) {
        this.repository = repository;
    }

    /** findById Repository-Methodenaufruf. Wenn das Repository die gesuchten Rechnungsdaten zurückgibt,
    * dann werden diese einfach weiter an den Grpc-Service weitergegeben, jedoch ist der Rückgabetyp im Repo
    * Optional, sprich es kann auch nichts zurückgegeben werden. In diesem Fall werfen wir durch
    * .orElseThrow() eine Fehlermeldung, welche dann ebenfalls im Grpc-Service gefangen wird. */
    public Rechnungsdaten findById(String rechnungsId) {
        return repository.findById(rechnungsId)
                .orElseThrow(() -> new RechnungNichtGefundenException("Keine Rechnung mit dieser ID gefunden."));
    }
}
