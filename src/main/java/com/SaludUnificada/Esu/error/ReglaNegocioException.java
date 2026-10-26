package com.SaludUnificada.Esu.error;

import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio lanzada cuando una operación viola una regla de negocio del sistema
 * (por ejemplo, superación de cupo máximo de sobreturnos diarios).
 */
public class ReglaNegocioException extends RuntimeException {

    private final HttpStatus status;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
        this.status = HttpStatus.UNPROCESSABLE_ENTITY;
    }

    public ReglaNegocioException(String mensaje, HttpStatus status) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
