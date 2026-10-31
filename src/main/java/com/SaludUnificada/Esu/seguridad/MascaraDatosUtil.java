package com.SaludUnificada.Esu.seguridad;

/**
 * Utilidad para el enmascaramiento y anonimización de datos sensibles
 * en registros de log y trazas operativas (cumplimiento Ley 25.326 de Protección de Datos Personales).
 */
public final class MascaraDatosUtil {

    private MascaraDatosUtil() {
    }

    /**
     * Enmascara una dirección de correo electrónico protegiendo la identidad del usuario.
     * Ejemplo: "paciente@hospital.gob.ar" -> "p***e@h***.gob.ar"
     */
    public static String enmascararEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String local = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex + 1);

        String localEnmascarado;
        if (local.length() <= 2) {
            localEnmascarado = local.charAt(0) + "***";
        } else {
            localEnmascarado = local.charAt(0) + "***" + local.charAt(local.length() - 1);
        }

        int dotIndex = domain.lastIndexOf('.');
        String domainEnmascarado;
        if (dotIndex > 1) {
            String sub = domain.substring(0, dotIndex);
            String ext = domain.substring(dotIndex);
            domainEnmascarado = sub.charAt(0) + "***" + ext;
        } else {
            domainEnmascarado = domain;
        }

        return localEnmascarado + "@" + domainEnmascarado;
    }
}
