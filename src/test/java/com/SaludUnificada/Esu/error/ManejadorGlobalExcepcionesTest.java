package com.SaludUnificada.Esu.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class ManejadorGlobalExcepcionesTest {

    private ManejadorGlobalExcepciones manejador;

    @BeforeEach
    void setUp() {
        manejador = new ManejadorGlobalExcepciones();
    }

    @Test
    void testNotFoundException() {
        NoEncontradoExcepcion ex = new NoEncontradoExcepcion("Paciente no encontrado con ID: 99");
        ResponseEntity<ErrorMessage> response = manejador.notFoundException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Paciente no encontrado con ID: 99", response.getBody().getMessage());
        assertEquals(HttpStatus.NOT_FOUND, response.getBody().getStatusCode());
    }

    @Test
    void testIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Dato inválido");
        ResponseEntity<ErrorMessage> response = manejador.handleIllegalArgumentException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Dato inválido", response.getBody().getMessage());
    }
}
