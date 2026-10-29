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

                try {
                    helper.setFrom(mailFrom, "ESU - Salud Unificada");
                } catch (Exception e) {
                    helper.setFrom(mailFrom);
                }
                helper.setTo(destinatario);
                helper.setSubject(asunto);
                message.addHeader("X-Mailer", "ESU-Platform/1.0");
                message.addHeader("X-Auto-Response-Suppress", "All");
                message.addHeader("Auto-Submitted", "auto-generated");
                String textoPlano = extraerTextoPlano(htmlContent);
                helper.setText(textoPlano, htmlContent);

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

    @Async
    public void enviarBienvenida(String email, String nombre) {
        if (email == null || email.isBlank()) {
            return;
        }
        String nombreFinal = (nombre != null && !nombre.isBlank()) ? nombre : email.split("@")[0];
        String htmlBody = construirHtmlBienvenida(nombreFinal);
        enviarCorreoHtml(email, "Bienvenido/a al Ecosistema de Salud Unificado (ESU)", htmlBody, null, null);
    }

    private String extraerTextoPlano(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private String construirHtmlBienvenida(String nombre) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <h2 style="color: #1e3a8a; margin: 0;">Ecosistema de Salud Unificado</h2>
                        <p style="color: #64748b; font-size: 14px; margin-top: 5px;">Portal de Salud Ciudadana - Cruz del Eje</p>
                    </div>
                    <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                    <h3 style="color: #0f172a;">Bienvenido/a, %s</h3>
                    <p style="color: #334155; line-height: 1.6;">
                        Tu cuenta ha sido creada exitosamente en la plataforma asistencial.
                        A partir de este momento puedes acceder a tu portal para gestionar turnos medicos,
                        consultar tu historial clinico, recetas digitales y acceder a tu carnet virtual.
                    </p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s/login" style="background-color: #2563eb; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Ingresar al Portal</a>
                    </div>
                    <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                    <p style="font-size: 12px; color: #94a3b8; text-align: center;">
                        Este es un correo automatico emitido por el sistema ESU. Si no te has registrado, por favor ignora este mensaje.
                    </p>
                </div>
            </body>
            </html>
        """.formatted(nombre, frontendUrl);
    }


    @Async
    public void enviarEmailVerificacion(String email, String nombre, String token) {
        if (email == null || email.isBlank()) {
            return;
        }
        String nombreFinal = (nombre != null && !nombre.isBlank()) ? nombre : email.split("@")[0];
        String linkVerificacion = frontendUrl + "/verificar-email?token=" + token;
        String htmlBody = construirHtmlVerificacionEmail(nombreFinal, linkVerificacion);
        enviarCorreoHtml(email, "Bienvenido/a a ESU - Confirma tu correo electrónico", htmlBody, null, null);
    }

    private String construirHtmlVerificacionEmail(String nombre, String linkVerificacion) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <h2 style="color: #0284c7; margin: 0;">Ecosistema de Salud Unificado</h2>
                        <p style="color: #64748b; font-size: 14px; margin-top: 5px;">Municipalidad de Cruz del Eje</p>
                    </div>
                    <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                    <h3 style="color: #0f172a;">Confirmación de Correo Electrónico</h3>
                    <p style="color: #334155; line-height: 1.6;">
                        Hola <strong>%s</strong>, para activar plenamente tu cuenta y acceder a tus turnos médicos, recetas digitales e historial clínico, confirma la titularidad de tu email haciendo clic en el siguiente enlace:
                    </p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #0284c7; color: #ffffff; padding: 14px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 15px; display: inline-block;">Verificar mi Correo</a>
                    </div>
                    <p style="color: #64748b; font-size: 13px; line-height: 1.5;">
                        Si el botón no funciona, copia y pega este enlace en tu navegador:<br>
                        <a href="%s" style="color: #0284c7; word-break: break-all;">%s</a>
                    </p>
                    <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                    <p style="font-size: 12px; color: #94a3b8; text-align: center;">
                        Este enlace tiene una vigencia limitada. Si no te has registrado en ESU, ignora este mensaje.
                    </p>
                </div>
            </body>
            </html>
        """.formatted(nombre, linkVerificacion, linkVerificacion, linkVerificacion);
    }


    @Async
    public void enviarCancelacionTurno(Turno turno, String motivo) {
        if (turno == null || turno.getPaciente() == null) {
            return;
        }

        String destinatario = obtenerEmailPaciente(turno);
        if (destinatario == null || destinatario.isBlank()) {
            return;
        }

        String pacienteNombre = "Estimado/a Paciente";
        try {
            pacienteNombre = turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido();
        } catch (Exception ignored) {
        }

        String medicoNombre = "Profesional Medico";
        try {
            if (turno.getProfesional() != null) {
                medicoNombre = turno.getProfesional().getNombre() + " " + turno.getProfesional().getApellido();
            }
        } catch (Exception ignored) {
        }

        String fechaHoraStr = turno.getFechaHora() != null ? turno.getFechaHora().format(DATE_TIME_FORMATTER) : "A convenir";
        String codigoCita = "TURNO-" + turno.getId();
        String motivoFinal = (motivo != null && !motivo.isBlank()) ? motivo : "Cancelacion administrativa";
        String linkNuevoTurno = frontendUrl + "/turnos";

        String htmlBody = construirHtmlTurnoCancelado(pacienteNombre, medicoNombre, fechaHoraStr, codigoCita, motivoFinal, linkNuevoTurno);
        enviarCorreoHtml(destinatario, "Cancelacion de Turno Medico - " + codigoCita, htmlBody, null, null);
    }

    private String construirHtmlTurnoCancelado(String paciente, String medico, String fechaHora, String codigo, String motivo, String linkNuevoTurno) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <h2 style="color: #dc2626; margin: 0;">Aviso de Cancelacion de Turno</h2>
                        <p style="color: #64748b; font-size: 14px; margin-top: 5px;">Ecosistema de Salud Unificado - Cruz del Eje</p>
                    </div>
                    <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                    <p style="color: #334155; line-height: 1.6;">Hola <strong>%s</strong>,</p>
                    <p style="color: #334155; line-height: 1.6;">Te informamos que tu turno medico ha sido cancelado en el sistema:</p>
                    <div style="background-color: #fef2f2; border-left: 4px solid #ef4444; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p style="margin: 5px 0;"><strong>Codigo de Cita:</strong> %s</p>
                        <p style="margin: 5px 0;"><strong>Fecha y Hora:</strong> %s hs</p>
                        <p style="margin: 5px 0;"><strong>Profesional:</strong> %s</p>
                        <p style="margin: 5px 0;"><strong>Motivo:</strong> %s</p>
                    </div>
                    <p style="color: #334155; line-height: 1.6;">Puedes ingresar a la plataforma para reprogramar una nueva consulta con tu profesional o especialidad requerida:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #0284c7; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Solicitar Nuevo Turno</a>
                    </div>
                    <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                    <p style="font-size: 12px; color: #94a3b8; text-align: center;">Notificacion automatica institucional - Municipalidad de Cruz del Eje.</p>
                </div>
            </body>
            </html>
        """.formatted(paciente, codigo, fechaHora, medico, motivo, linkNuevoTurno);
    }

}
