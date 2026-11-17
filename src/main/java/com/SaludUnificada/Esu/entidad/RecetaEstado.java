package com.SaludUnificada.Esu.entidad;

public enum RecetaEstado {
    PENDIENTE,
    DISPENSADA,
    VENCIDA,
    ANULADA;

    public boolean puedeTransicionarA(RecetaEstado nuevoEstado) {
        if (this == nuevoEstado) return true;
        return switch (this) {
            case PENDIENTE -> nuevoEstado == DISPENSADA || nuevoEstado == VENCIDA || nuevoEstado == ANULADA;
            case DISPENSADA, VENCIDA, ANULADA -> false;
        };
    }

    public static RecetaEstado desdeString(String valor) {
        if (valor == null || valor.isBlank()) return PENDIENTE;
        try {
            return RecetaEstado.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDIENTE;
        }
    }
}
