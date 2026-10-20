package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

public class EvolucionInmutableListener {

    @PostLoad
    public void onPostLoad(EvolucionMedica evolucion) {
        // Marcamos si ya estaba firmada en base de datos
    }

    @PreUpdate
    public void preUpdate(EvolucionMedica evolucion) {
        if (Boolean.TRUE.equals(evolucion.getFirmada())) {
            throw new IllegalStateException(
                "Ley 26.529 de Derechos del Paciente: La evolución médica (ID: " + evolucion.getId() +
                ") está firmada electrónicamente y es legalmente inmutable. No se permite modificación directa; debe emitirse una Adenda clínica."
            );
        }
    }

    @PreRemove
    public void preRemove(EvolucionMedica evolucion) {
        if (Boolean.TRUE.equals(evolucion.getFirmada())) {
            throw new IllegalStateException(
                "Ley 26.529 de Derechos del Paciente: La evolución médica (ID: " + evolucion.getId() +
                ") está firmada electrónicamente y no puede ser eliminada del expediente clínico."
            );
        }
    }
}
