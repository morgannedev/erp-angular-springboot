package com.algedro.exception;

/**
 * HTTP 409 — Intento de crear una subcategoría de tercer nivel.
 * Data-Model.md §1.4: "La restricción de máximo dos niveles se aplica
 * a nivel de aplicación (Spring Boot), no en BD" → A7 de technical-design.md.
 */
public class NivelMaximoExcedidoException extends RuntimeException {
    public NivelMaximoExcedidoException() {
        super("No se puede crear una categoría de tercer nivel. El máximo permitido es nivel 1 (subcategoría directa de raíz).");
    }
}