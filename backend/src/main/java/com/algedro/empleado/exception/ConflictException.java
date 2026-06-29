package com.algedro.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // Mapea automáticamente a un error HTTP 409
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}