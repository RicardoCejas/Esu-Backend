package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.servicio.ITurnoServicio;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SobreturnoControladorTest {

    @Mock
    private ITurnoServicio turnoServicio;

    @InjectMocks
    private TurnoControlador turnoControlador;

    private SobreturnoDtoRequest request;
    private TurnoDtoResponse response;

    @BeforeEach
    void setUp() {
        request = SobreturnoDtoRequest.builder()
                .profesionalId(1L)
                .pacienteId(10L)
                .fechaHora(LocalDateTime.now().plusDays(1).withHour(11).withMinute(30))
                .motivoSobreturno("Urgencia menor en sala")
                .build();

        response = TurnoDtoResponse.builder()
                .id(99L)
                .fechaHora(request.getFechaHora())
                .estado("PENDIENTE")
                .esSobreturno(true)
                .motivoSobreturno("Urgencia menor en sala")
                .build();
    }

    @Test
    @DisplayName("POST /api/turnos/sobreturno debe responder 201 CREATED")
    void testCrearSobreturno_Endpoint() {
        when(turnoServicio.crearSobreturno(any(SobreturnoDtoRequest.class))).thenReturn(response);

        ResponseEntity<TurnoDtoResponse> res = turnoControlador.crearSobreturno(request);

        assertNotNull(res);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertTrue(res.getBody().getEsSobreturno());
        assertEquals(99L, res.getBody().getId());
        verify(turnoServicio).crearSobreturno(any());
    }
}
