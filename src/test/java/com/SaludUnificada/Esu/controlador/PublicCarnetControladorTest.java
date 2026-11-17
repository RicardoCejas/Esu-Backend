package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.CarnetDigitalDtoResponse;
import com.SaludUnificada.Esu.servicio.CarnetYFamiliarServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicCarnetControladorTest {

    @Mock
    private CarnetYFamiliarServicio carnetYFamiliarServicio;

    @InjectMocks
    private PublicCarnetControlador publicCarnetControlador;

    private CarnetDigitalDtoResponse carnetMock;

    @BeforeEach
    void setUp() {
        carnetMock = CarnetDigitalDtoResponse.builder()
                .pacienteId(10L)
                .nombreCompleto("Lucas Agustín Ramírez")
                .dni("39412850")
                .obraSocial("APROSS (Córdoba)")
                .nroAfiliado("20-39412850-4/01")
                .grupoSanguineo("0 Positivo (0+)")
                .fechaEmision(LocalDate.now())
                .estadoActivo(true)
                .tokenVerificacion("VALID-HASH-123")
                .qrPayload("ESU:10:HASH")
                .build();
    }

    @Test
    @DisplayName("Debe verificar credencial pública por ID de paciente exitosamente")
    void verificarPublico_PorId_Exitoso() {
        when(carnetYFamiliarServicio.emitirCarnetDigital(10L)).thenReturn(carnetMock);

        ResponseEntity<?> response = publicCarnetControlador.verificarPublico(10L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(carnetMock, response.getBody());
        verify(carnetYFamiliarServicio).emitirCarnetDigital(10L);
    }

    @Test
    @DisplayName("Debe verificar credencial pública por DNI exitosamente")
    void verificarPublico_PorDni_Exitoso() {
        when(carnetYFamiliarServicio.emitirCarnetDigitalPorDni("39412850")).thenReturn(carnetMock);

        ResponseEntity<?> response = publicCarnetControlador.verificarPublico(null, "39412850");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(carnetMock, response.getBody());
        verify(carnetYFamiliarServicio).emitirCarnetDigitalPorDni("39412850");
    }

    @Test
    @DisplayName("Debe responder BAD_REQUEST si no se pasa ni ID ni DNI")
    void verificarPublico_SinParametros_BadRequest() {
        ResponseEntity<?> response = publicCarnetControlador.verificarPublico(null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Debe obtener credencial pública por ID de ruta exitosamente")
    void obtenerPublicoPorId_Exitoso() {
        when(carnetYFamiliarServicio.emitirCarnetDigital(10L)).thenReturn(carnetMock);

        ResponseEntity<CarnetDigitalDtoResponse> response = publicCarnetControlador.obtenerPublicoPorId(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Lucas Agustín Ramírez", response.getBody().getNombreCompleto());
        verify(carnetYFamiliarServicio).emitirCarnetDigital(10L);
    }
}
