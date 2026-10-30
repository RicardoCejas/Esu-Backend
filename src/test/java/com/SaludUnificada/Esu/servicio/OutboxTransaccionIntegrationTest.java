package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.OutboxEvento;
import com.SaludUnificada.Esu.repositorio.OutboxEventoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class OutboxTransaccionIntegrationTest {

    @Autowired
    private OutboxTransaccionServicio outboxTransaccionServicio;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @Autowired
    private OutboxEventoRepositorio outboxEventoRepositorio;

    @MockBean
    private EmailNotificacionServicio emailNotificacionServicio;

    @BeforeEach
    void setUp() {
        outboxEventoRepositorio.deleteAll();
    }

    @Test
    @DisplayName("Concurrencia: Múltiples hilos reclamando eventos no procesan el mismo registro simultáneamente")
    void testReclamarEventos_ConcurrenciaSinDuplicados() throws Exception {
        int totalEventos = 10;
        LocalDateTime ahora = outboxTransaccionServicio.ahora();

        for (int i = 0; i < totalEventos; i++) {
            OutboxEvento ev = new OutboxEvento();
            ev.setTipoEvento("TURNO_CREADO");
            ev.setAggregateId((long) (100 + i));
            ev.setPayload("{\"test\":\"concurrency\"}");
            ev.setEstado("PENDIENTE");
            ev.setIntentos(0);
            ev.setFechaCreacion(ahora.minusMinutes(1));
            ev.setFechaActualizacion(ahora.minusMinutes(1));
            outboxEventoRepositorio.save(ev);
        }

        int hilos = 3;
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<List<Long>>> futures = new ArrayList<>();

        for (int h = 0; h < hilos; h++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                List<OutboxEvento> claimed = outboxTransaccionServicio.reclamarEventosParaProcesar(10);
                return claimed.stream().map(OutboxEvento::getId).toList();
            }));
        }

        startLatch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        List<Long> todosLosReclamados = new ArrayList<>();
        for (Future<List<Long>> future : futures) {
            todosLosReclamados.addAll(future.get());
        }

        assertEquals(totalEventos, todosLosReclamados.size(), "Todos los eventos deben ser reclamados sin pérdidas");
        assertEquals(totalEventos, todosLosReclamados.stream().distinct().count(), "No deben existir reclamaciones duplicadas");
    }

    @Test
    @DisplayName("Recuperación de lease vencido: Evento abandonado en EN_PROCESO es re-reclamado y su lease extendido")
    void testRecuperacionLeaseVencido() {
        LocalDateTime ahora = outboxTransaccionServicio.ahora();

        OutboxEvento ev = new OutboxEvento();
        ev.setTipoEvento("TURNO_CREADO");
        ev.setAggregateId(200L);
        ev.setPayload("{\"test\":\"lease\"}");
        ev.setEstado("EN_PROCESO");
        ev.setLeaseHasta(ahora.minusMinutes(5)); // Lease vencido hace 5 minutos
        ev.setIntentos(1);
        ev.setFechaCreacion(ahora.minusMinutes(10));
        ev.setFechaActualizacion(ahora.minusMinutes(5));
        OutboxEvento guardado = outboxEventoRepositorio.save(ev);

        List<OutboxEvento> reclamados = outboxTransaccionServicio.reclamarEventosParaProcesar(10);

        assertFalse(reclamados.isEmpty());
        OutboxEvento reReclamado = reclamados.stream()
                .filter(e -> e.getId().equals(guardado.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(reReclamado);
        assertEquals("EN_PROCESO", reReclamado.getEstado());
        assertEquals(2, reReclamado.getIntentos());
        assertTrue(reReclamado.getLeaseHasta().isAfter(ahora));
    }

    @Test
    @DisplayName("Idempotencia externa: Si ya fue despachado externamente, no vuelve a enviar correo y pasa a ENVIADO")
    void testIdempotenciaExterna_NoReenviaCorreo() {
        LocalDateTime ahora = outboxTransaccionServicio.ahora();

        OutboxEvento ev = new OutboxEvento();
        ev.setTipoEvento("TURNO_CREADO");
        ev.setAggregateId(300L);
        ev.setPayload("{\"test\":\"idempotency\"}");
        ev.setEstado("EN_PROCESO");
        ev.setDespachadoExterno(true); // Ya fue enviado al servidor de correo previamente
        ev.setIntentos(1);
        ev.setFechaCreacion(ahora);
        ev.setFechaActualizacion(ahora);
        OutboxEvento guardado = outboxEventoRepositorio.save(ev);

        outboxScheduler.procesarEventoIndividual(guardado);

        OutboxEvento actualizado = outboxEventoRepositorio.findById(guardado.getId()).orElseThrow();
        assertEquals("ENVIADO", actualizado.getEstado());
        assertTrue(actualizado.getDespachadoExterno());
        verifyNoInteractions(emailNotificacionServicio);
    }

    @Test
    @DisplayName("Inconsistencia: Evento sin turno asociado pasa a INCONSISTENTE_NO_ENCONTRADO y no figura enviado")
    void testEntidadInexistente_MarcaInconsistente() {
        LocalDateTime ahora = outboxTransaccionServicio.ahora();

        OutboxEvento ev = new OutboxEvento();
        ev.setTipoEvento("TURNO_CREADO");
        ev.setAggregateId(999999L); // Turno inexistente
        ev.setPayload("{\"test\":\"inconsistency\"}");
        ev.setEstado("EN_PROCESO");
        ev.setIntentos(1);
        ev.setFechaCreacion(ahora);
        ev.setFechaActualizacion(ahora);
        OutboxEvento guardado = outboxEventoRepositorio.save(ev);

        outboxScheduler.procesarEventoIndividual(guardado);

        OutboxEvento actualizado = outboxEventoRepositorio.findById(guardado.getId()).orElseThrow();
        assertEquals("INCONSISTENTE", actualizado.getEstado());
        assertNotNull(actualizado.getUltimoError());
        assertTrue(actualizado.getUltimoError().contains("Turno no encontrado"));
        assertNull(actualizado.getLeaseHasta());
    }

    @Test
    @DisplayName("Backoff y fallo terminal: Evento con 3 intentos pasa a estado final FALLIDO")
    void testReintentosMaximos_PasaAFallido() {
        LocalDateTime ahora = outboxTransaccionServicio.ahora();

        OutboxEvento ev = new OutboxEvento();
        ev.setTipoEvento("TURNO_CREADO");
        ev.setAggregateId(400L);
        ev.setPayload("{\"test\":\"backoff\"}");
        ev.setEstado("EN_PROCESO");
        ev.setIntentos(2);
        ev.setFechaCreacion(ahora);
        ev.setFechaActualizacion(ahora);
        OutboxEvento guardado = outboxEventoRepositorio.save(ev);

        outboxTransaccionServicio.marcarFallo(guardado.getId(), "Fallo persistente SMTP", 3);

        OutboxEvento actualizado = outboxEventoRepositorio.findById(guardado.getId()).orElseThrow();
        assertEquals("FALLIDO", actualizado.getEstado());
        assertNull(actualizado.getProximoIntento());
        assertEquals("Fallo persistente SMTP", actualizado.getUltimoError());
    }
}
