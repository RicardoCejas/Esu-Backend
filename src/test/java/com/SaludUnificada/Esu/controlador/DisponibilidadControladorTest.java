package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.DisponibilidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.DisponibilidadDtoResponse;
import com.SaludUnificada.Esu.servicio.IDisponibilidadServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadControladorTest {

    @Mock
    private IDisponibilidadServicio disponibilidadServicio;

    @InjectMocks
    private DisponibilidadControlador disponibilidadControlador;

    private DisponibilidadDtoRequest request;
    private DisponibilidadDtoResponse response;

    @BeforeEach
    void setUp() {
        request = DisponibilidadDtoRequest.builder()
                .profesionalId(1L)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .intervaloMinutos(20)
                .centroSaludId(1L)
                .build();

        response = DisponibilidadDtoResponse.builder()
                .id(1L)
                .profesionalId(1L)
                .nombreProfesional("Carlos Martínez")
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .intervaloMinutos(20)
                .slotsGenerados(36)
                .build();
    }

    @Test
    @DisplayName("POST /api/disponibilidades debe retornar 201 CREATED con el DTO de respuesta")
    void testConfigurarDisponibilidad_Endpoint() {
        when(disponibilidadServicio.configurarDisponibilidad(any())).thenReturn(response);

        ResponseEntity<DisponibilidadDtoResponse> resultado = disponibilidadControlador.configurarDisponibilidad(request);

        assertNotNull(resultado);
        assertEquals(HttpStatus.CREATED, resultado.getStatusCode());
        assertEquals(1L, resultado.getBody().getId());
        assertEquals(36, resultado.getBody().getSlotsGenerados());
        verify(disponibilidadServicio).configurarDisponibilidad(any());
    }

    @Test
    @DisplayName("GET /api/profesionales/{id}/disponibilidad debe retornar 200 OK con la lista de disponibilidades")
    void testObtenerDisponibilidadPorProfesional_Endpoint() {
        when(disponibilidadServicio.obtenerDisponibilidadPorProfesional(1L)).thenReturn(List.of(response));

        ResponseEntity<List<DisponibilidadDtoResponse>> resultado = disponibilidadControlador.obtenerDisponibilidadPorProfesional(1L);

        assertNotNull(resultado);
        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertEquals(1, resultado.getBody().size());
        verify(disponibilidadServicio).obtenerDisponibilidadPorProfesional(1L);
    }
}
