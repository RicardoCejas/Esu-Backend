package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.BloqueoAgendaDtoRequest;
import com.SaludUnificada.Esu.dto.response.BloqueoAgendaDtoResponse;
import com.SaludUnificada.Esu.dto.response.TurnoAfectadoDto;
import com.SaludUnificada.Esu.servicio.IBloqueoAgendaServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueoAgendaControladorTest {

    @Mock
    private IBloqueoAgendaServicio bloqueoAgendaServicio;

    @InjectMocks
    private BloqueoAgendaControlador bloqueoAgendaControlador;

    private BloqueoAgendaDtoRequest request;
    private BloqueoAgendaDtoResponse response;

    @BeforeEach
    void setUp() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(2).withHour(8).withMinute(0);
        LocalDateTime fin = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0);

        request = BloqueoAgendaDtoRequest.builder()
                .fechaInicio(inicio)
                .fechaFin(fin)
                .motivo("Licencia por Capacitación")
                .build();

        TurnoAfectadoDto afectado = TurnoAfectadoDto.builder()
                .turnoId(10L)
                .fechaHora(inicio.plusHours(1))
                .pacienteId(5L)
                .nombrePaciente("Carlos Gómez")
                .dniPaciente("28999000")
                .telefonoPaciente("03549-421122")
                .estadoAnterior("CONFIRMADO")
                .build();

        response = BloqueoAgendaDtoResponse.builder()
                .id(1L)
                .profesionalId(2L)
                .nombreProfesional("Lucía González")
                .fechaInicio(inicio)
                .fechaFin(fin)
                .motivo("Licencia por Capacitación")
                .turnosAfectados(1)
                .turnosCancelados(List.of(afectado))
                .build();
    }

    @Test
    @DisplayName("POST /api/profesionales/{id}/bloquear-agenda debe retornar 201 CREATED")
    void testBloquearAgenda_Endpoint() {
        when(bloqueoAgendaServicio.registrarBloqueo(eq(2L), any())).thenReturn(response);

        ResponseEntity<BloqueoAgendaDtoResponse> res = bloqueoAgendaControlador.bloquearAgenda(2L, request);

        assertNotNull(res);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals(1L, res.getBody().getId());
        assertEquals(1, res.getBody().getTurnosAfectados());
        verify(bloqueoAgendaServicio).registrarBloqueo(eq(2L), any());
    }

    @Test
    @DisplayName("GET /api/profesionales/{id}/bloqueos debe retornar 200 OK con la lista de bloqueos")
    void testListarBloqueosPorProfesional_Endpoint() {
        when(bloqueoAgendaServicio.listarBloqueosPorProfesional(2L)).thenReturn(List.of(response));

        ResponseEntity<List<BloqueoAgendaDtoResponse>> res = bloqueoAgendaControlador.listarBloqueosPorProfesional(2L);

        assertNotNull(res);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
        verify(bloqueoAgendaServicio).listarBloqueosPorProfesional(2L);
    }
}
