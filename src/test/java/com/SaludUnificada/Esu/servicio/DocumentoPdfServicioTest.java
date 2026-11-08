package com.SaludUnificada.Esu.servicio;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentoPdfServicioTest {

    @Test
    void laFirmaHmacDetectaContenidoAlterado() {
        DocumentoPdfServicio servicio = new DocumentoPdfServicio();
        ReflectionTestUtils.setField(servicio, "firmaSecret", "secreto-documental-de-pruebas-2026");

        String contenido = "EVOLUCION|30111222|MP-34982|Motivo|Diagnostico|Tratamiento";
        String firma = servicio.calcularFirmaHmacSha256(contenido);

        assertTrue(servicio.verificarFirmaHmacSha256(contenido, firma));
        assertFalse(servicio.verificarFirmaHmacSha256(contenido + "|alterado", firma));
    }
}
