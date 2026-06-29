package com.algedro.exception;

/** HTTP 404 — Recurso no encontrado */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}