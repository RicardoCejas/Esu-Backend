package com.SaludUnificada.Esu.seguridad;

import java.util.List;
import java.util.Set;

/**
 * Governance rules for center-scoped PBAC permissions.
 * Prevents center reception accounts from granting administrative or network-wide privileges.
 */
public final class PermisoGobernanzaPolicy {

    public static final Set<String> RESTRICTED_CENTER_PERMISSIONS = Set.of(
            "PACIENTES_FUSIONAR",
            "USUARIOS_ADMINISTRAR",
            "CONSULTORIOS_ADMINISTRAR"
    );

    public static final List<String> DEFAULT_CENTER_RECEPTIONIST_PERMISSIONS = List.of(
            "TURNOS_VER",
            "TURNOS_CREAR",
            "TURNOS_CANCELAR",
            "TURNOS_REAGENDAR",
            "SOBRETURNOS_CREAR",
            "SALA_ESPERA_GESTIONAR",
            "PACIENTES_VER",
            "PACIENTES_REGISTRAR",
            "CONSULTORIOS_VER",
            "MEDICOS_VER",
            "MEDICOS_ASIGNAR_CONSULTORIO",
            "REPORTES_VER"
    );

    public static final List<String> DEFAULT_CENTER_PROFESSIONAL_PERMISSIONS = List.of(
            "TURNOS_VER",
            "PACIENTES_VER",
            "CONSULTORIOS_VER",
            "MEDICOS_VER"
    );

    private PermisoGobernanzaPolicy() {
    }

    public static boolean isRestrictedForCenter(String permissionCode) {
        if (permissionCode == null) {
            return false;
        }
        return RESTRICTED_CENTER_PERMISSIONS.contains(permissionCode.trim().toUpperCase());
    }

    public static List<String> filterCenterPermissions(List<String> permissionCodes) {
        if (permissionCodes == null) {
            return List.of();
        }
        return permissionCodes.stream()
                .filter(code -> code != null && !code.isBlank() && !isRestrictedForCenter(code))
                .map(code -> code.trim().toUpperCase())
                .distinct()
                .toList();
    }

    public static List<String> resolveCenterCreatePermissions(String normalizedRole, List<String> requestedPermissions) {
        if (requestedPermissions != null && !requestedPermissions.isEmpty()) {
            return filterCenterPermissions(requestedPermissions);
        }
        String roleClean = normalizedRole == null ? "" : normalizedRole.trim().toUpperCase();
        if ("PROFESIONAL".equals(roleClean) || "MEDICO".equals(roleClean)) {
            return DEFAULT_CENTER_PROFESSIONAL_PERMISSIONS;
        }
        return DEFAULT_CENTER_RECEPTIONIST_PERMISSIONS;
    }
}
