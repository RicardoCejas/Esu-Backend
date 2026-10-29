package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.OutboxEvento;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.repositorio.OutboxEventoRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private NotificacionUsuarioServicio notificacionUsuarioServicio;

    @InjectMocks
    private OutboxScheduler outboxScheduler;

    @Test
    void testProcesarEventosPendientes_ExitosoReceta() {
        OutboxEvento evento = new OutboxEvento();
        evento.setId(1L);
        evento.setTipoEvento("RECETA_EMITIDA");
        evento.setAggregateId(10L);
        evento.setPayload("payload-qr");
        evento.setEstado("PENDIENTE");
        evento.setFechaCreacion(LocalDateTime.now());

        RecetaDigital receta = new RecetaDigital();
        receta.setId(10L);

        when(outboxEventoRepositorio.findByEstadoForUpdate(eq("PENDIENTE"), any(PageRequest.class)))
                .thenReturn(List.of(evento));
        when(recetaRepositorio.findById(10L)).thenReturn(Optional.of(receta));
        byte[] pdfBytes = new byte[]{1, 2, 3};
        when(pdfServicio.generarRecetaPdf(receta, "payload-qr")).thenReturn(pdfBytes);

        outboxScheduler.procesarEventosPendientes();

        assertEquals("ENVIADO", evento.getEstado());
        verify(emailServicio).enviarRecetaDigital(receta, pdfBytes);
        verify(outboxEventoRepositorio).saveAll(List.of(evento));
    }

    @Test
    void testProcesarEventosPendientes_TipoDesconocido_MarcaErrorTipoDesconocido() {
        OutboxEvento evento = new OutboxEvento();
        evento.setId(2L);
        evento.setTipoEvento("TIPO_DESCONOCIDO_INVALIDO");
        evento.setAggregateId(20L);
        evento.setEstado("PENDIENTE");
        evento.setIntentos(0);

        when(outboxEventoRepositorio.findByEstadoForUpdate(eq("PENDIENTE"), any(PageRequest.class)))
                .thenReturn(List.of(evento));

        outboxScheduler.procesarEventosPendientes();

        assertEquals("ERROR_TIPO_DESCONOCIDO", evento.getEstado());
        assertEquals(1, evento.getIntentos());
        verifyNoInteractions(emailServicio);
        verify(outboxEventoRepositorio).saveAll(List.of(evento));
    }

    @Test
    void testProcesarEventosPendientes_FalloReintentosMaximos_MarcaFallido() {
        OutboxEvento evento = new OutboxEvento();
        evento.setId(3L);
        evento.setTipoEvento("TURNO_CREADO");
        evento.setAggregateId(30L);
        evento.setEstado("PENDIENTE");
        evento.setIntentos(2);

        Turno turno = new Turno();
        turno.setId(30L);

        when(outboxEventoRepositorio.findByEstadoForUpdate(eq("PENDIENTE"), any(PageRequest.class)))
                .thenReturn(List.of(evento));
        when(turnoRepositorio.findById(30L)).thenReturn(Optional.of(turno));
        doThrow(new RuntimeException("SMTP down")).when(emailServicio).enviarConfirmacionTurno(turno);

        outboxScheduler.procesarEventosPendientes();

        assertEquals("FALLIDO", evento.getEstado());
        assertEquals(3, evento.getIntentos());
        assertEquals("SMTP down", evento.getUltimoError());
        verify(outboxEventoRepositorio).saveAll(List.of(evento));
    }
}
