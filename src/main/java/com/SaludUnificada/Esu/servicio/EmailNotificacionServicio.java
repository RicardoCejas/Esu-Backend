package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.entidad.Turno;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailNotificacionServicio {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificacionServicio.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:notificaciones@esu.cruzdeleje.gob.ar}")
    private String mailFrom;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Envío asíncrono de correo de confirmación de turno con link formal de cancelación.
     */
    @Async
    public void enviarConfirmacionTurno(Turno turno) {
        if (turno == null || turno.getPaciente() == null) {
            log.warn("No se pudo despachar el correo de confirmación: el turno o paciente es nulo.");
            return;
        }

        String destinatario = obtenerEmailPaciente(turno);
        if (destinatario == null || destinatario.isBlank()) {
            log.info("El paciente ID {} no posee email asociado. Se omite notificación por correo.", turno.getPaciente().getId());
            return;
        }

        String pacienteNombre = "";
        try {
            pacienteNombre = turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido();
        } catch (Exception e) {
            pacienteNombre = "Estimado/a Paciente";
        }

        String medicoNombre = "Profesional Médico";
        try {
            if (turno.getProfesional() != null) {
                medicoNombre = turno.getProfesional().getNombre() + " " + turno.getProfesional().getApellido();
            }
        } catch (Exception e) {
            medicoNombre = "Profesional de Guardia";
        }

        String especialidad = "Medicina General";
        try {
            if (turno.getProfesional() != null && turno.getProfesional().getEspecialidades() != null && !turno.getProfesional().getEspecialidades().isEmpty()) {
                especialidad = turno.getProfesional().getEspecialidades().iterator().next().getNombre();
            }
        } catch (Exception e) {
            log.debug("No se pudo resolver especialidad de forma diferida: {}", e.getMessage());
        }
        String centroNombre = "Hospital / Centro de Salud Cruz del Eje";
        String fechaHoraStr = turno.getFechaHora() != null ? turno.getFechaHora().format(DATE_TIME_FORMATTER) : "A confirmar";
        String codigoCita = "TURNO-" + turno.getId();
        String linkCancelacion = frontendUrl + "/mis-turnos?cancelar=" + turno.getId();

        String htmlBody = construirHtmlTurnoConfirmado(pacienteNombre, medicoNombre, especialidad, centroNombre, fechaHoraStr, codigoCita, linkCancelacion);

        enviarCorreoHtml(destinatario, "Confirmación de Turno Médico - " + codigoCita, htmlBody, null, null);
    }

    /**
     * Envío asíncrono de receta médica digital con el comprobante PDF oficial adjunto.
     */
    @Async
    public void enviarRecetaDigital(RecetaDigital receta, byte[] pdfAdjunto) {
        if (receta == null || receta.getPaciente() == null) {
            log.warn("No se pudo despachar la receta digital: datos insuficientes.");
            return;
        }

        String destinatario = (receta.getPaciente().getUsuario() != null)
                ? receta.getPaciente().getUsuario().getEmail()
                : null;

        if (destinatario == null || destinatario.isBlank()) {
            log.info("El paciente de la receta ID {} no posee email asociado para despacho digital.", receta.getId());
            return;
        }

        String pacienteNombre = receta.getPaciente().getNombre() + " " + receta.getPaciente().getApellido();
        String medicoNombre = (receta.getProfesional() != null)
                ? "Dr./Dra. " + receta.getProfesional().getNombre() + " " + receta.getProfesional().getApellido()
                : "Profesional Médico";
        String matricula = (receta.getProfesional() != null) ? receta.getProfesional().getMatricula() : "S/D";
        String fechaEmision = receta.getFechaEmision() != null ? receta.getFechaEmision().format(DATE_FORMATTER) : "";
        String fechaVencimiento = receta.getFechaVencimiento() != null ? receta.getFechaVencimiento().format(DATE_FORMATTER) : "";
        String codigoReceta = "RECETA-" + receta.getId();

        String htmlBody = construirHtmlRecetaDigital(pacienteNombre, medicoNombre, matricula, fechaEmision, fechaVencimiento, codigoReceta, receta.getHashIntegridad());

        enviarCorreoHtml(destinatario, "Receta Médica Digital Oficial - " + codigoReceta, htmlBody, pdfAdjunto, "Receta_" + codigoReceta + ".pdf");
    }

    /**
     * Envío de token de recuperación de contraseña con enlace seguro.
     */
    @Async
    public void enviarTokenRecuperacion(String email, String nombreUsuario, String token) {
        if (email == null || email.isBlank()) {
            return;
        }

        String linkReseteo = frontendUrl + "/reset-password?token=" + token;
        String htmlBody = construirHtmlRecuperacion(nombreUsuario != null ? nombreUsuario : email, linkReseteo);

        enviarCorreoHtml(email, "Restablecimiento de Contraseña - ESU Cruz del Eje", htmlBody, null, null);
    }

    private String obtenerEmailPaciente(Turno turno) {
        try {
            if (turno.getPaciente() != null && turno.getPaciente().getUsuario() != null) {
                return turno.getPaciente().getUsuario().getEmail();
            }
        } catch (Exception e) {
            log.debug("Error accediendo diferido al email del paciente: {}", e.getMessage());
        }
        return null;
    }

    private void enviarCorreoHtml(String destinatario, String asunto, String htmlContent, byte[] adjuntoBytes, String nombreAdjunto) {
        if (mailSender == null) {
            log.warn("JavaMailSender no está configurado. Simulación preventiva de envío de correo a: {}", destinatario);
            return;
        }

        int intentos = 0;
        int maxIntentos = 3;
        boolean enviado = false;

        while (intentos < maxIntentos && !enviado) {
            intentos++;
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(mailFrom);
                helper.setTo(destinatario);
                helper.setSubject(asunto);
                helper.setText(htmlContent, true);

                if (adjuntoBytes != null && nombreAdjunto != null) {
                    helper.addAttachment(nombreAdjunto, new ByteArrayResource(adjuntoBytes));
                }

                mailSender.send(message);
                enviado = true;
                log.info("Notificación por correo despachada exitosamente a {} en intento {}", destinatario, intentos);
            } catch (Exception ex) {
                log.error("Fallo al enviar correo a {} en intento {}/{}: {}", destinatario, intentos, maxIntentos, ex.getMessage());
                if (intentos < maxIntentos) {
                    try {
                        Thread.sleep(1000L * intentos);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (!enviado) {
            log.warn("PREVENTIVO: No se pudo entregar el correo a {} luego de {} intentos. La operación principal continúa sin bloqueos.", destinatario, maxIntentos);
        }
    }

    private String construirHtmlTurnoConfirmado(String paciente, String medico, String especialidad, String centro, String fechaHora, String codigo, String linkCancelacion) {
        return "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif; background-color: #f4f8fb; margin: 0; padding: 20px; color: #1e293b;'>"
                + "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #cbd5e1; padding: 25px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);'>"
                + "<div style='border-bottom: 2px solid #0284c7; padding-bottom: 12px; margin-bottom: 20px; text-align: center;'>"
                + "<h2 style='color: #0284c7; margin: 0;'>ESU - Cruz del Eje</h2>"
                + "<p style='margin: 4px 0 0; font-size: 14px; color: #64748b;'>Ecosistema de Salud Unificado</p>"
                + "</div>"
                + "<p>Hola <strong>" + paciente + "</strong>,</p>"
                + "<p>Tu turno médico ha sido <strong>confirmado exitosamente</strong>. A continuación, los detalles de tu cita:</p>"
                + "<div style='background: #f8fafc; border-left: 4px solid #0284c7; padding: 15px; margin: 20px 0; border-radius: 4px;'>"
                + "<p style='margin: 5px 0;'><strong>Código de Cita:</strong> " + codigo + "</p>"
                + "<p style='margin: 5px 0;'><strong>Fecha y Hora:</strong> " + fechaHora + " hs</p>"
                + "<p style='margin: 5px 0;'><strong>Profesional:</strong> " + medico + "</p>"
                + "<p style='margin: 5px 0;'><strong>Especialidad:</strong> " + especialidad + "</p>"
                + "<p style='margin: 5px 0;'><strong>Centro Médico:</strong> " + centro + "</p>"
                + "</div>"
                + "<p style='font-size: 13px; color: #475569;'>Te recomendamos presentarte 15 minutos antes con tu DNI y credencial médica en recepción.</p>"
                + "<div style='margin-top: 25px; text-align: center;'>"
                + "<a href='" + linkCancelacion + "' style='display: inline-block; background: #e11d48; color: #ffffff; text-decoration: none; padding: 10px 18px; border-radius: 6px; font-weight: bold; font-size: 13px;'>Cancelar Turno</a>"
                + "</div>"
                + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 25px 0;'/>"
                + "<p style='font-size: 11px; color: #94a3b8; text-align: center;'>Este es un mensaje institucional generado automáticamente por la Municipalidad de Cruz del Eje.</p>"
                + "</div></body></html>";
    }

    private String construirHtmlRecetaDigital(String paciente, String medico, String matricula, String emision, String vencimiento, String codigo, String hash) {
        return "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif; background-color: #f4f8fb; margin: 0; padding: 20px; color: #1e293b;'>"
                + "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #cbd5e1; padding: 25px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);'>"
                + "<div style='border-bottom: 2px solid #059669; padding-bottom: 12px; margin-bottom: 20px; text-align: center;'>"
                + "<h2 style='color: #059669; margin: 0;'>ESU - Receta Médica Oficial</h2>"
                + "<p style='margin: 4px 0 0; font-size: 14px; color: #64748b;'>Documento Electrónico Firmado (Ley 27.553)</p>"
                + "</div>"
                + "<p>Estimado/a <strong>" + paciente + "</strong>,</p>"
                + "<p>Se ha emitido una nueva <strong>Receta Médica Digital</strong> a tu nombre. Adjunto a este correo encontrarás el documento oficial en formato PDF con su respectivo código QR y Hash criptográfico de validación.</p>"
                + "<div style='background: #f0fdf4; border-left: 4px solid #059669; padding: 15px; margin: 20px 0; border-radius: 4px;'>"
                + "<p style='margin: 5px 0;'><strong>Identificador:</strong> " + codigo + "</p>"
                + "<p style='margin: 5px 0;'><strong>Médico Prescriptor:</strong> " + medico + " (MP: " + matricula + ")</p>"
                + "<p style='margin: 5px 0;'><strong>Fecha Emisión:</strong> " + emision + "</p>"
                + "<p style='margin: 5px 0;'><strong>Vencimiento:</strong> " + vencimiento + "</p>"
                + "<p style='margin: 5px 0; word-break: break-all; font-size: 11px; color: #64748b;'><strong>Hash SHA-256:</strong> " + hash + "</p>"
                + "</div>"
                + "<p style='font-size: 13px; color: #475569;'>Presentá este comprobante o su versión digital en cualquier farmacia adherida de la red institucional.</p>"
                + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 25px 0;'/>"
                + "<p style='font-size: 11px; color: #94a3b8; text-align: center;'>Municipalidad de Cruz del Eje - Secretaría de Salud Pública.</p>"
                + "</div></body></html>";
    }

    private String construirHtmlRecuperacion(String nombre, String link) {
        return "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif; background-color: #f4f8fb; margin: 0; padding: 20px; color: #1e293b;'>"
                + "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #cbd5e1; padding: 25px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);'>"
                + "<div style='border-bottom: 2px solid #0284c7; padding-bottom: 12px; margin-bottom: 20px; text-align: center;'>"
                + "<h2 style='color: #0284c7; margin: 0;'>ESU - Seguridad de la Cuenta</h2>"
                + "<p style='margin: 4px 0 0; font-size: 14px; color: #64748b;'>Recuperación de Contraseña</p>"
                + "</div>"
                + "<p>Hola <strong>" + nombre + "</strong>,</p>"
                + "<p>Hemos recibido una solicitud para restablecer tu contraseña de acceso a la plataforma ESU.</p>"
                + "<p>Hacé clic en el siguiente botón para definir una nueva clave. Este enlace es válido por <strong>15 minutos</strong> y puede utilizarse una sola vez:</p>"
                + "<div style='margin: 30px 0; text-align: center;'>"
                + "<a href='" + link + "' style='display: inline-block; background: #0284c7; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-weight: bold; font-size: 14px;'>Restablecer Contraseña</a>"
                + "</div>"
                + "<p style='font-size: 12px; color: #64748b;'>Si vos no realizaste esta solicitud, desestimá este correo. Tu cuenta permanece protegida.</p>"
                + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 25px 0;'/>"
                + "<p style='font-size: 11px; color: #94a3b8; text-align: center;'>Seguridad Informática - Municipalidad de Cruz del Eje.</p>"
                + "</div></body></html>";
    }
}
