package com.SaludUnificada.Esu.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NoEncontradoExcepcion extends RuntimeException {
    private final HttpStatus status;

    public NoEncontradoExcepcion(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }
}