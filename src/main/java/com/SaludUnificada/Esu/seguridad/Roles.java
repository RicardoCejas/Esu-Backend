package com.SaludUnificada.Esu.seguridad;

import java.util.Set;

/**
 * Issue #119: conjunto único de roles del sistema.
 * Cualquier valor fuera de esta lista se considera inválido y no debe autenticarse.
 */
public final class Roles {

    public static final String PACIENTE = "PACIENTE";
    public static final String PROFESIONAL = "PROFESIONAL";
    public static final String RECEPCIONISTA = "RECEPCIONISTA";
    public static final String ADMIN = "ADMIN";

    public static final Set<String> VALIDOS = Set.of(PACIENTE, PROFESIONAL, RECEPCIONISTA, ADMIN);

    private Roles() {
    }

    public static String normalizar(String rol) {
        if (rol == null || rol.isBlank()) {
            return null;
        }

        String normalizado = rol.trim().toUpperCase();
        if (normalizado.startsWith("ROLE_")) {
            normalizado = normalizado.substring(5);
        }

        if ("MEDICO".equals(normalizado) || "DOCTOR".equals(normalizado)) {
            return PROFESIONAL;
        }
        if ("USUARIO".equals(normalizado)) {
            return PACIENTE;
        }

        return VALIDOS.contains(normalizado) ? normalizado : null;
    }

    public static String requerido(String rol) {
        String normalizado = normalizar(rol);
        if (normalizado == null) {
            throw new IllegalArgumentException("Rol no reconocido: " + rol);
        }
        return normalizado;
    }
}
