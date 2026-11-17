package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.NotificacionDtoResponse;
import com.SaludUnificada.Esu.entidad.NotificacionUsuario;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.NotificacionUsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificacionUsuarioServicioTest {

    @Mock
    private NotificacionUsuarioRepositorio notificacionRepositorio;

    private NotificacionUsuarioServicio notificacionServicio;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        notificacionServicio = new NotificacionUsuarioServicio(notificacionRepositorio);
    }

    @Test
    void crearNotificacionPersisteNotificacionNoLeida() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setEmail("paciente@correo.com");

        notificacionServicio.crearNotificacion(usuario, "Turno Confirmado", "Su cita está agendada.", "TURNO", "/mis-turnos");

        verify(notificacionRepositorio, times(1)).save(any(NotificacionUsuario.class));
    }

    @Test
    void listarParaUsuarioRetornaDtosMapeados() {
        Usuario usuario = new Usuario();
        usuario.setEmail("test@correo.com");

        NotificacionUsuario notif = NotificacionUsuario.builder()
                .id(1L)
                .usuario(usuario)
                .titulo("Aviso")
                .mensaje("Mensaje test")
                .tipo("SISTEMA")
                .leida(false)
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(notificacionRepositorio.findByUsuarioEmailOrderByFechaCreacionDesc("test@correo.com"))
                .thenReturn(List.of(notif));

        List<NotificacionDtoResponse> dtos = notificacionServicio.listarParaUsuario("test@correo.com");

        assertNotNull(dtos);
        assertEquals(1, dtos.size());
        assertEquals("Aviso", dtos.get(0).getTitulo());
        assertFalse(dtos.get(0).getLeida());
    }

    @Test
    void marcarComoLeidaActualizaFlag() {
        Usuario usuario = new Usuario();
        usuario.setEmail("test@correo.com");

        NotificacionUsuario notif = NotificacionUsuario.builder()
                .id(10L)
                .usuario(usuario)
                .leida(false)
                .build();

        when(notificacionRepositorio.findById(10L)).thenReturn(Optional.of(notif));

        notificacionServicio.marcarComoLeida(10L, "test@correo.com");

        assertTrue(notif.getLeida());
        verify(notificacionRepositorio).save(notif);
    }
}
