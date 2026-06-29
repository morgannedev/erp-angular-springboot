package com.algedro.exception;

/** HTTP 409 — Conflicto de unicidad (NIF, EAN, username duplicados, nivel jerarquía...) */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}