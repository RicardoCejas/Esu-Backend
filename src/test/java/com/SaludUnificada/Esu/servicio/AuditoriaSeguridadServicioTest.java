package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.LogAuditoriaSeguridad;
import com.SaludUnificada.Esu.repositorio.LogAuditoriaSeguridadRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaSeguridadServicioTest {

    @Mock
    private LogAuditoriaSeguridadRepositorio auditoriaRepositorio;

    @InjectMocks
    private AuditoriaSeguridadServicio auditoriaServicio;

    @Test
    void registrarLoginExitoso_guardaEntidadConEventoLoginOk() {
        auditoriaServicio.registrarLoginExitoso("admin@esu.gob.ar");
        verify(auditoriaRepositorio, times(1)).save(argThat(log ->
                log.getEmail().equals("admin@esu.gob.ar") &&
                log.getEvento() == LogAuditoriaSeguridad.EventoSeguridad.LOGIN_OK
        ));
    }

    @Test
    void registrarLoginFallido_guardaEntidadConEventoLoginFail() {
        auditoriaServicio.registrarLoginFallido("admin@esu.gob.ar", "Password erróneo");
        verify(auditoriaRepositorio, times(1)).save(argThat(log ->
                log.getEmail().equals("admin@esu.gob.ar") &&
                log.getEvento() == LogAuditoriaSeguridad.EventoSeguridad.LOGIN_FAIL &&
                log.getDetalle().equals("Password erróneo")
        ));
    }

    @Test
    void estaBloqueadoPorIntentosFallidos_5FallosConsecutivos_retornaTrue() {
        List<LogAuditoriaSeguridad> eventos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            LogAuditoriaSeguridad l = new LogAuditoriaSeguridad();
            l.setEvento(LogAuditoriaSeguridad.EventoSeguridad.LOGIN_FAIL);
            l.setEmail("hacker@malicioso.com");
            l.setFecha(LocalDateTime.now().minusMinutes(i));
            eventos.add(l);
        }

        when(auditoriaRepositorio.findEventosRecientesPorEmail(eq("hacker@malicioso.com"), any(LocalDateTime.class)))
                .thenReturn(eventos);

        boolean bloqueado = auditoriaServicio.estaBloqueadoPorIntentosFallidos("hacker@malicioso.com");
        assertTrue(bloqueado);
    }

    @Test
    void estaBloqueadoPorIntentosFallidos_menosDe5Fallos_retornaFalse() {
        List<LogAuditoriaSeguridad> eventos = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            LogAuditoriaSeguridad l = new LogAuditoriaSeguridad();
            l.setEvento(LogAuditoriaSeguridad.EventoSeguridad.LOGIN_FAIL);
            l.setEmail("usuario@normal.com");
            eventos.add(l);
        }

        when(auditoriaRepositorio.findEventosRecientesPorEmail(eq("usuario@normal.com"), any(LocalDateTime.class)))
                .thenReturn(eventos);

        boolean bloqueado = auditoriaServicio.estaBloqueadoPorIntentosFallidos("usuario@normal.com");
        assertFalse(bloqueado);
    }

    @Test
    void estaBloqueadoPorIntentosFallidos_loginExitosoIntermedio_retornaFalse() {
        List<LogAuditoriaSeguridad> eventos = new ArrayList<>();
        // 2 fallos recientes
        for (int i = 0; i < 2; i++) {
            LogAuditoriaSeguridad l = new LogAuditoriaSeguridad();
            l.setEvento(LogAuditoriaSeguridad.EventoSeguridad.LOGIN_FAIL);
            eventos.add(l);
        }
        // 1 exito previo
        LogAuditoriaSeguridad ok = new LogAuditoriaSeguridad();
        ok.setEvento(LogAuditoriaSeguridad.EventoSeguridad.LOGIN_OK);
        eventos.add(ok);

        when(auditoriaRepositorio.findEventosRecientesPorEmail(eq("usuario@normal.com"), any(LocalDateTime.class)))
                .thenReturn(eventos);

        boolean bloqueado = auditoriaServicio.estaBloqueadoPorIntentosFallidos("usuario@normal.com");
        assertFalse(bloqueado);
    }
}
