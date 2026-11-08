package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.VerificacionPublicaDocumentoDtoResponse;
import com.SaludUnificada.Esu.dto.response.VerificacionRecetaPublicaDtoResponse;
import com.SaludUnificada.Esu.servicio.EvolucionMedicaServicio;
import com.SaludUnificada.Esu.servicio.RecetaDigitalServicio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicoVerificacionControladorTest {

    @Mock
    private RecetaDigitalServicio recetaServicio;

    @Mock
    private EvolucionMedicaServicio evolucionServicio;

    @InjectMocks
    private PublicoVerificacionControlador publicoVerificacionControlador;

    @Test
    void testVerificarDocumento_HashVacio_RetornaInvalido() {
        ResponseEntity<VerificacionPublicaDocumentoDtoResponse> resp = publicoVerificacionControlador.verificarDocumento("   ");

        assertNotNull(resp.getBody());
        assertFalse(resp.getBody().isValido());
        assertFalse(resp.getBody().isIntegridadValida());
        assertTrue(resp.getBody().getMensaje().contains("no proporcionado"));
    }

    @Test
    void testVerificarDocumento_RecetaValida_RetornaDtoUnificado() {
        String hash = "hash_receta_123456";
        VerificacionRecetaPublicaDtoResponse recetaResp = VerificacionRecetaPublicaDtoResponse.builder()
                .valido(true)
                .integridadValida(true)
                .tipoDocumento("RECETA_DIGITAL")
                .estado("VIGENTE")
                .fechaEmision(LocalDate.of(2026, 11, 7))
                .fechaVencimiento(LocalDate.of(2026, 12, 7))
                .matriculaProfesional("MP-1234")
                .hashIntegridadSha256(hash)
                .mensaje("Documento oficial validado.")
                .build();

        when(recetaServicio.verificarDocumentoPublico(hash)).thenReturn(recetaResp);

        ResponseEntity<VerificacionPublicaDocumentoDtoResponse> resp = publicoVerificacionControlador.verificarDocumento(hash);

        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isValido());
        assertTrue(resp.getBody().isIntegridadValida());
        assertEquals("RECETA_DIGITAL", resp.getBody().getTipoDocumento());
        assertEquals("VIGENTE", resp.getBody().getEstado());
        assertEquals("MP-1234", resp.getBody().getMatriculaProfesional());
        assertEquals(hash, resp.getBody().getHashIntegridadSha256());
    }

    @Test
    void testVerificarDocumento_EvolucionValida_RetornaDtoUnificado() {
        String hash = "hash_evolucion_456789";
        when(recetaServicio.verificarDocumentoPublico(hash)).thenReturn(new VerificacionRecetaPublicaDtoResponse());

        Map<String, Object> evResp = new LinkedHashMap<>();
        evResp.put("valido", true);
        evResp.put("integridadValida", true);
        evResp.put("tipoDocumento", "EVOLUCION_MEDICA");
        evResp.put("fechaFirma", "2026-11-07T12:00:00");
        evResp.put("matriculaProfesional", "MP-5678");
        evResp.put("hashIntegridad", hash);
        evResp.put("mensaje", "Evolución clínica validada.");

        when(evolucionServicio.verificarDocumentoPublico(hash)).thenReturn(evResp);

        ResponseEntity<VerificacionPublicaDocumentoDtoResponse> resp = publicoVerificacionControlador.verificarDocumento(hash);

        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isValido());
        assertTrue(resp.getBody().isIntegridadValida());
        assertEquals("EVOLUCION_MEDICA", resp.getBody().getTipoDocumento());
        assertEquals("MP-5678", resp.getBody().getMatriculaProfesional());
        assertEquals(hash, resp.getBody().getHashIntegridadSha256());
    }

    @Test
    void testVerificarDocumento_AdendaValida_RetornaDtoUnificado() {
        String hash = "hash_adenda_112233";
        when(recetaServicio.verificarDocumentoPublico(hash)).thenReturn(new VerificacionRecetaPublicaDtoResponse());

        Map<String, Object> adendaResp = new LinkedHashMap<>();
        adendaResp.put("valido", true);
        adendaResp.put("integridadValida", true);
        adendaResp.put("tipoDocumento", "EVOLUCION_ADENDA");
        adendaResp.put("fechaFirma", "2026-11-07T14:30:00");
        adendaResp.put("matriculaProfesional", "MP-5678");
        adendaResp.put("hashIntegridad", hash);
        adendaResp.put("mensaje", "Adenda validada.");

        when(evolucionServicio.verificarDocumentoPublico(hash)).thenReturn(adendaResp);

        ResponseEntity<VerificacionPublicaDocumentoDtoResponse> resp = publicoVerificacionControlador.verificarDocumento(hash);

        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isValido());
        assertEquals("EVOLUCION_ADENDA", resp.getBody().getTipoDocumento());
        assertEquals("MP-5678", resp.getBody().getMatriculaProfesional());
    }

    @Test
    void testVerificarDocumento_NoEncontrado_RetornaRespuestaSegura() {
        String hash = "hash_inexistente_999999";
        when(recetaServicio.verificarDocumentoPublico(hash)).thenReturn(new VerificacionRecetaPublicaDtoResponse());
        when(evolucionServicio.verificarDocumentoPublico(hash)).thenReturn(Collections.emptyMap());

        ResponseEntity<VerificacionPublicaDocumentoDtoResponse> resp = publicoVerificacionControlador.verificarDocumento(hash);

        assertNotNull(resp.getBody());
        assertFalse(resp.getBody().isValido());
        assertFalse(resp.getBody().isIntegridadValida());
        assertNull(resp.getBody().getTipoDocumento());
        assertTrue(resp.getBody().getMensaje().contains("No se encontró ningún documento"));
    }
}
