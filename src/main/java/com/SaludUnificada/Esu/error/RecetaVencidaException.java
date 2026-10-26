package com.SaludUnificada.Esu.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class RecetaVencidaException extends RuntimeException {
    public RecetaVencidaException(String message) {
        super(message);
    }
}
