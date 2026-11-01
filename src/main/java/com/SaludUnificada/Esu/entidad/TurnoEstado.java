package com.SaludUnificada.Esu.entidad;

public enum TurnoEstado {
    DISPONIBLE,
    PENDIENTE,
    CONFIRMADO,
    EN_SALA_DE_ESPERA,
    ATENDIENDO,
    ATENDIDO,
    AUSENTE,
    CANCELADO;

    public boolean puedeTransicionarA(TurnoEstado nuevoEstado) {
        if (this == nuevoEstado) return true;
        return switch (this) {
            case DISPONIBLE -> nuevoEstado == PENDIENTE || nuevoEstado == CONFIRMADO || nuevoEstado == CANCELADO;
            case PENDIENTE -> nuevoEstado == CONFIRMADO || nuevoEstado == EN_SALA_DE_ESPERA || nuevoEstado == CANCELADO || nuevoEstado == AUSENTE;
            case CONFIRMADO -> nuevoEstado == EN_SALA_DE_ESPERA || nuevoEstado == CANCELADO || nuevoEstado == AUSENTE;
            case EN_SALA_DE_ESPERA -> nuevoEstado == ATENDIENDO || nuevoEstado == ATENDIDO || nuevoEstado == AUSENTE || nuevoEstado == CANCELADO;
            case ATENDIENDO -> nuevoEstado == ATENDIDO || nuevoEstado == CANCELADO || nuevoEstado == AUSENTE;
            case ATENDIDO, CANCELADO, AUSENTE -> false;
        };
    }

    public static TurnoEstado desdeString(String valor) {
        if (valor == null || valor.isBlank()) return PENDIENTE;
        String clean = valor.trim().toUpperCase();
        if ("EN_ESPERA".equals(clean)) return EN_SALA_DE_ESPERA;
        if ("COMPLETADO".equals(clean) || "FINALIZADO".equals(clean)) return ATENDIDO;
        if ("CANCELADO_POR_MEDICO".equals(clean)) return CANCELADO;
        try {
            return TurnoEstado.valueOf(clean);
        } catch (IllegalArgumentException e) {
            return PENDIENTE;
        }
    }
}
