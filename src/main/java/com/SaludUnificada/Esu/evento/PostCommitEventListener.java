package com.SaludUnificada.Esu.evento;

import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import com.SaludUnificada.Esu.servicio.DocumentoPdfServicio;
import com.SaludUnificada.Esu.servicio.EmailNotificacionServicio;
import com.SaludUnificada.Esu.servicio.NotificacionUsuarioServicio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCommitEventListener {

    private final RecetaDigitalRepositorio recetaRepositorio;
    private final TurnoRepositorio turnoRepositorio;
    private final DocumentoPdfServicio pdfServicio;
    private final EmailNotificacionServicio emailServicio;
    private final NotificacionUsuarioServicio notificacionUsuarioServicio;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecetaEmitida(RecetaEmitidaEvent event) {
        try {
            recetaRepositorio.findById(event.getRecetaId()).ifPresent(receta -> {
                if (pdfServicio != null && emailServicio != null) {
                    try {
                        byte[] pdfBytes = pdfServicio.generarRecetaPdf(receta, event.getQrUrl());
                        emailServicio.enviarRecetaDigital(receta, pdfBytes);
                        log.info("Notificación por email de receta #{} enviada satisfactoriamente tras commit.", receta.getId());
                    } catch (Exception e) {
                        log.warn("Fallo al generar PDF o despachar email de receta post-commit: {}", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error al procesar evento RecetaEmitidaEvent: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTurnoCreado(TurnoCreadoEvent event) {
        try {
            turnoRepositorio.findById(event.getTurnoId()).ifPresent(turno -> {
                if (notificacionUsuarioServicio != null && turno.getPaciente() != null && turno.getPaciente().getUsuario() != null) {
                    try {
                        notificacionUsuarioServicio.crearNotificacion(
                                turno.getPaciente().getUsuario(),
                                "Turno Confirmado",
                                "Su turno médico ha sido confirmado para el " +
                                        turno.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                                "TURNO",
                                "/mis-turnos"
                        );
                    } catch (Exception e) {
                        log.warn("Fallo al registrar notificación interna de turno post-commit: {}", e.getMessage());
                    }
                }

                if (emailServicio != null) {
                    try {
                        emailServicio.enviarConfirmacionTurno(turno);
                        log.info("Email de confirmación para turno #{} enviado tras commit exitoso.", turno.getId());
                    } catch (Exception e) {
                        log.warn("Fallo al despachar email de turno post-commit: {}", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error al procesar evento TurnoCreadoEvent: {}", e.getMessage());
        }
    }
}
