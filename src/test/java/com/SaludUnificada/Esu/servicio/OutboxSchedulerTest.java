package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.OutboxEvento;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.repositorio.OutboxEventoRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

    @Mock
    private OutboxEventoRepositorio outboxEventoRepositorio;

    @Mock
    private RecetaDigitalRepositorio recetaRepositorio;

    @Mock
    private TurnoRepositorio turnoRepositorio;

    @Mock
    private DocumentoPdfServicio pdfServicio;

    @Mock
    private EmailNotificacionServicio emailServicio;

    private final ZoneId zoneId = ZoneId.of("America/Argentina/Cordoba");
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), zoneId);

    private OutboxScheduler outboxScheduler;

    @BeforeEach
    void setUp() {
        outboxScheduler = new OutboxScheduler(
                outboxEventoRepositorio,
                recetaRepositorio,
                turnoRepositorio,
                pdfServicio,
                emailServicio,
                clock,
                zoneId
        );
    }

    @Test
    void testProcesarEventosPendientes_ExitosoReceta() {
        OutboxEvento evento = new OutboxEvento();
        evento.setId(1L);
        evento.setTipoEvento("RECETA_EMITIDA");
        evento.setAggregateId(10L);
        evento.setPayload("payload-qr");
        evento.setEstado("PENDIENTE");
        evento.setFechaCreacion(LocalDateTime.now(clock));

        RecetaDigital receta = new RecetaDigital();
        receta.setId(10L);

        when(outboxEventoRepositorio.reclamarEventosDisponibles(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(evento));
        when(outboxEventoRepositorio.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        when(recetaRepositorio.findById(10L)).thenReturn(Optional.of(receta));
        byte[] pdfBytes = new byte[]{1, 2, 3};
        when(pdfServicio.generarRecetaPdf(receta, "payload-qr")).thenReturn(pdfBytes);
        when(outboxEventoRepositorio.findById(1L)).thenReturn(Optional.of(evento));

        outboxScheduler.procesarEventosPendientes();

        assertEquals("ENVIADO", evento.getEstado());
        assertNull(evento.getLeaseHasta());
        assertNull(evento.getProximoIntento());
        verify(emailServicio).enviarRecetaDigital(receta, pdfBytes);
    }

    @Test
    void testProcesarEventosPendientes_EntidadNoEncontrada_MarcaInconsistente() {
        OutboxEvento evento = new OutboxEvento();
        evento.setId(2L);
        evento.setTipoEvento("TURNO_CREADO");
        evento.setAggregateId(999L);
        evento.setEstado("PENDIENTE");

        when(outboxEventoRepositorio.reclamarEventosDisponibles(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(evento));
        when(outboxEventoRepositorio.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(turnoRepositorio.findById(999L)).thenReturn(Optional.empty());
        when(outboxEventoRepositorio.findById(2L)).thenReturn(Optional.of(evento));

        outboxScheduler.procesarEventosPendientes();

        assertEquals("INCONSISTENTE_NO_ENCONTRADO", evento.getEstado());
        assertNotNull(evento.getUltimoError());
        verifyNoInteractions(emailServicio);
    }

    @Test
    void testProcesarEventosPendientes_TipoDesconocido_MarcaErrorTipoDesconocido() {
        OutboxEvento evento = new OutboxEvento();
        evento.setId(3L);
        evento.setTipoEvento("TIPO_DESCONOCIDO_INVALIDO");
        evento.setAggregateId(20L);
        evento.setEstado("PENDIENTE");

        when(outboxEventoRepositorio.reclamarEventosDisponibles(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(evento));
        when(outboxEventoRepositorio.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventoRepositorio.findById(3L)).thenReturn(Optional.of(evento));

        outboxScheduler.procesarEventosPendientes();

        assertEquals("ERROR_TIPO_DESCONOCIDO", evento.getEstado());
        verifyNoInteractions(emailServicio);
    }

    @Test
    void testProcesarEventosPendientes_FalloReintentosMaximos_MarcaFallido() {
        OutboxEvento evento = new OutboxEvento();
        evento.setId(4L);
        evento.setTipoEvento("TURNO_CREADO");
        evento.setAggregateId(30L);
        evento.setEstado("PENDIENTE");
        evento.setIntentos(2); // Al reclamar pasará a 3

        Turno turno = new Turno();
        turno.setId(30L);

        when(outboxEventoRepositorio.reclamarEventosDisponibles(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(evento));
        when(outboxEventoRepositorio.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(turnoRepositorio.findById(30L)).thenReturn(Optional.of(turno));
        doThrow(new RuntimeException("SMTP unreachable")).when(emailServicio).enviarConfirmacionTurno(turno);
        when(outboxEventoRepositorio.findById(4L)).thenReturn(Optional.of(evento));

        outboxScheduler.procesarEventosPendientes();

        assertEquals("FALLIDO", evento.getEstado());
        assertEquals(3, evento.getIntentos());
        assertEquals("SMTP unreachable", evento.getUltimoError());
    }

    @Test
    void testReintentarEventoManual() {
        OutboxEvento evento = new OutboxEvento();
        evento.setId(5L);
        evento.setEstado("FALLIDO");
        evento.setIntentos(3);
        evento.setUltimoError("Error previo");

        when(outboxEventoRepositorio.findById(5L)).thenReturn(Optional.of(evento));

        boolean ok = outboxScheduler.reintentarEventoManual(5L);

        assertTrue(ok);
        assertEquals("PENDIENTE", evento.getEstado());
        assertEquals(0, evento.getIntentos());
        assertNull(evento.getUltimoError());
        assertNull(evento.getLeaseHasta());
        assertNull(evento.getProximoIntento());
        verify(outboxEventoRepositorio).save(evento);
    }
}
