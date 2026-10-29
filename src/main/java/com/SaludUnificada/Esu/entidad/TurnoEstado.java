package com.SaludUnificada.Esu.entidad;

public enum TurnoEstado {
    DISPONIBLE,
    PENDIENTE,
    CONFIRMADO,
    EN_SALA_DE_ESPERA,
    ATENDIDO,
    AUSENTE,
    CANCELADO;

    public boolean puedeTransicionarA(TurnoEstado nuevoEstado) {
        if (this == nuevoEstado) return true;
        return switch (this) {
            case DISPONIBLE -> nuevoEstado == PENDIENTE || nuevoEstado == CONFIRMADO || nuevoEstado == CANCELADO;
            case PENDIENTE -> nuevoEstado == CONFIRMADO || nuevoEstado == EN_SALA_DE_ESPERA || nuevoEstado == CANCELADO || nuevoEstado == AUSENTE;
            case CONFIRMADO -> nuevoEstado == EN_SALA_DE_ESPERA || nuevoEstado == CANCELADO || nuevoEstado == AUSENTE;
            case EN_SALA_DE_ESPERA -> nuevoEstado == ATENDIDO || nuevoEstado == AUSENTE || nuevoEstado == CANCELADO;
            case ATENDIDO, CANCELADO, AUSENTE -> false;
        };
    }

    public static TurnoEstado desdeString(String valor) {
        if (valor == null || valor.isBlank()) return PENDIENTE;
        try {
            return TurnoEstado.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDIENTE;
        }
    }
}
