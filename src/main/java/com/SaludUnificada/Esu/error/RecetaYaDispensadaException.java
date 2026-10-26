package com.SaludUnificada.Esu.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RecetaYaDispensadaException extends RuntimeException {
    public RecetaYaDispensadaException(String message) {
        super(message);
    }
}
