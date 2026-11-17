package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.OutboxEventoDtoResponse;
import com.SaludUnificada.Esu.servicio.OutboxScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxControladorTest {

    @Mock
    private OutboxScheduler outboxScheduler;

    @InjectMocks
    private OutboxControlador outboxControlador;

    @Test
    void testListarFallidos() {
        OutboxEventoDtoResponse ev = OutboxEventoDtoResponse.builder()
                .id(10L)
                .estado("FALLIDO")
                .tipoEvento("TURNO_CONFIRMADO")
                .aggregateId(100L)
                .build();

        when(outboxScheduler.listarEventosConProblemas(any(PageRequest.class)))
                .thenReturn(List.of(ev));

        ResponseEntity<List<OutboxEventoDtoResponse>> response = outboxControlador.listarFallidos(0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("FALLIDO", response.getBody().get(0).getEstado());
        verify(outboxScheduler).listarEventosConProblemas(any(PageRequest.class));
    }

    @Test
    void testReintentarEvento() {
        when(outboxScheduler.reintentarEventoManual(15L)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = outboxControlador.reintentarEvento(15L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("reintentado"));
        verify(outboxScheduler).reintentarEventoManual(15L);
    }
}