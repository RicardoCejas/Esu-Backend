package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.EvolucionInmutableListener;
import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvolucionInmutableTest {

    @Test
    void testEvolucionFirmadaNoPuedeModificarse() {
        EvolucionMedica evolucion = new EvolucionMedica();
        evolucion.setId(100L);
        evolucion.setFirmada(true);

        EvolucionInmutableListener listener = new EvolucionInmutableListener();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            listener.preUpdate(evolucion);
        });

        assertTrue(ex.getMessage().contains("Ley 26.529"));
        assertTrue(ex.getMessage().contains("inmutable"));
    }

    @Test
    void testEvolucionFirmadaNoPuedeEliminarse() {
        EvolucionMedica evolucion = new EvolucionMedica();
        evolucion.setId(100L);
        evolucion.setFirmada(true);

        EvolucionInmutableListener listener = new EvolucionInmutableListener();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            listener.preRemove(evolucion);
        });

        assertTrue(ex.getMessage().contains("Ley 26.529"));
        assertTrue(ex.getMessage().contains("eliminada"));
    }
}
