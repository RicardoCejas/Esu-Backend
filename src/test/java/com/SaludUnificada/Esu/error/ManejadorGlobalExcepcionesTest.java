package com.SaludUnificada.Esu.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

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
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/pacientes/99"));
        ResponseEntity<ProblemDetail> response = manejador.handleNoEncontrado(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Paciente no encontrado con ID: 99", response.getBody().getDetail());
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
