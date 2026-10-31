package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.NotificacionDtoResponse;
import com.SaludUnificada.Esu.entidad.NotificacionUsuario;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.NotificacionUsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionUsuarioServicio {

    private final NotificacionUsuarioRepositorio notificacionRepositorio;

    @Transactional
    public void crearNotificacion(Usuario usuario, String titulo, String mensaje, String tipo, String linkAccion) {
        if (usuario == null) {
            log.warn("No se puede registrar notificaciÃ³n: el usuario destinatario es nulo.");
            return;
        }

        NotificacionUsuario notif = NotificacionUsuario.builder()
                .usuario(usuario)
                .titulo(titulo)
                .mensaje(mensaje)
                .tipo(tipo != null ? tipo : "SISTEMA")
                .leida(false)
                .linkAccion(linkAccion)
                .build();

        notificacionRepositorio.save(notif);
        log.info("NotificaciÃ³n registrada para el usuario {}: {}", usuario.getEmail(), titulo);
    }

    @Transactional(readOnly = true)
    public List<NotificacionDtoResponse> listarParaUsuario(String email, Boolean soloNoLeidas) {
        if (Boolean.TRUE.equals(soloNoLeidas)) {
            return notificacionRepositorio.findByUsuarioEmailAndLeidaFalseOrderByFechaCreacionDesc(email)
                    .stream()
                    .map(this::paraDto)
                    .collect(Collectors.toList());
        }
        return listarParaUsuario(email);
    }

    @Transactional(readOnly = true)
    public List<NotificacionDtoResponse> listarParaUsuario(String email) {
        return notificacionRepositorio.findByUsuarioEmailOrderByFechaCreacionDesc(email)
                .stream()
                .map(this::paraDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(String email) {
        return notificacionRepositorio.countByUsuarioEmailAndLeidaFalse(email);
    }

    @Transactional
    public void marcarComoLeida(Long notificacionId, String email) {
        NotificacionUsuario notif = notificacionRepositorio.findById(notificacionId)
                .orElseThrow(() -> new NoEncontradoExcepcion("NotificaciÃ³n no encontrada con ID: " + notificacionId));

        if (notif.getUsuario() != null && !notif.getUsuario().getEmail().equalsIgnoreCase(email)) {
            throw new org.springframework.security.access.AccessDeniedException("No posee autorizaciÃ³n para modificar notificaciones de otro usuario.");
        }

        notif.setLeida(true);
        notificacionRepositorio.save(notif);
    }

    @Transactional
    public void marcarTodasComoLeidas(String email) {
        notificacionRepositorio.marcarTodasComoLeidas(email);
        log.info("Todas las notificaciones marcadas como leÃ­das para el usuario: {}", email);
    }

    private NotificacionDtoResponse paraDto(NotificacionUsuario notif) {
        return NotificacionDtoResponse.builder()
                .id(notif.getId())
                .titulo(notif.getTitulo())
                .mensaje(notif.getMensaje())
                .tipo(notif.getTipo())
                .leida(notif.getLeida())
                .fechaCreacion(notif.getFechaCreacion())
                .linkAccion(notif.getLinkAccion())
                .build();
    }

    @Transactional
    public void eliminarNotificacion(Long notificacionId, String email) {
        NotificacionUsuario notif = notificacionRepositorio.findById(notificacionId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Notificacion no encontrada con ID: " + notificacionId));

        if (notif.getUsuario() != null && !notif.getUsuario().getEmail().equalsIgnoreCase(email)) {
            throw new org.springframework.security.access.AccessDeniedException("No posee autorizacion para eliminar notificaciones de otro usuario.");
        }

        notificacionRepositorio.delete(notif);
    }

    @Transactional
    public void eliminarTodasLeidas(String email) {
        notificacionRepositorio.eliminarTodasLeidas(email);
    }
}
