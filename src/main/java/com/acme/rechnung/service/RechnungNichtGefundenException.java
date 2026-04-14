/* Selbstgecoded.*/
package com.acme.rechnung.service;

public final class RechnungNichtGefundenException extends RuntimeException {
    public RechnungNichtGefundenException(String nachricht) {
        super(nachricht);
    }
}
