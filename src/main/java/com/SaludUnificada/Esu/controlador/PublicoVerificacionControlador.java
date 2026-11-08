package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.VerificacionPublicaDocumentoDtoResponse;
import com.SaludUnificada.Esu.dto.response.VerificacionRecetaPublicaDtoResponse;
import com.SaludUnificada.Esu.servicio.EvolucionMedicaServicio;
import com.SaludUnificada.Esu.servicio.RecetaDigitalServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Verificación Pública", description = "Endpoint canónico unificado para verificar la autenticidad e integridad de recetas, evoluciones y adendas clínicas")
public class PublicoVerificacionControlador {

    @Autowired
    private RecetaDigitalServicio recetaServicio;

    @Autowired
    private EvolucionMedicaServicio evolucionServicio;

    @GetMapping("/verificar/{hash}")
    @Operation(summary = "Verificar autenticidad e integridad de cualquier documento clínico por su hash o QR")
    public ResponseEntity<VerificacionPublicaDocumentoDtoResponse> verificarDocumento(@PathVariable String hash) {
        if (hash == null || hash.isBlank()) {
            return ResponseEntity.ok(VerificacionPublicaDocumentoDtoResponse.builder()
                    .valido(false)
                    .integridadValida(false)
                    .mensaje("Código de verificación no proporcionado.")
                    .build());
        }

        String hashLimpio = hash.trim();

        // 1. Probar en recetas digitales
        VerificacionRecetaPublicaDtoResponse recetaResp = recetaServicio.verificarDocumentoPublico(hashLimpio);
        if (recetaResp != null && recetaResp.getTipoDocumento() != null) {
            return ResponseEntity.ok(VerificacionPublicaDocumentoDtoResponse.builder()
                    .valido(recetaResp.isValido())
                    .integridadValida(recetaResp.isIntegridadValida())
                    .tipoDocumento(recetaResp.getTipoDocumento())
                    .estado(recetaResp.getEstado())
                    .fechaEmision(recetaResp.getFechaEmision() != null ? recetaResp.getFechaEmision().toString() : null)
                    .fechaVencimiento(recetaResp.getFechaVencimiento() != null ? recetaResp.getFechaVencimiento().toString() : null)
                    .farmaciaDispensadora(recetaResp.getFarmaciaDispensadora())
                    .matriculaProfesional(recetaResp.getMatriculaProfesional())
                    .hashIntegridadSha256(recetaResp.getHashIntegridadSha256())
                    .mensaje(recetaResp.getMensaje())
                    .build());
        }

        // 2. Probar en evoluciones médicas y adendas
        Map<String, Object> evResp = evolucionServicio.verificarDocumentoPublico(hashLimpio);
        if (evResp != null && evResp.containsKey("tipoDocumento")) {
            return ResponseEntity.ok(VerificacionPublicaDocumentoDtoResponse.builder()
                    .valido(Boolean.TRUE.equals(evResp.get("valido")))
                    .integridadValida(Boolean.TRUE.equals(evResp.get("integridadValida")))
                    .tipoDocumento((String) evResp.get("tipoDocumento"))
                    .fechaFirma(evResp.get("fechaFirma") != null ? evResp.get("fechaFirma").toString() : null)
                    .matriculaProfesional((String) evResp.get("matriculaProfesional"))
                    .hashIntegridadSha256((String) evResp.get("hashIntegridad"))
                    .mensaje((String) evResp.get("mensaje"))
                    .build());
        }

        return ResponseEntity.ok(VerificacionPublicaDocumentoDtoResponse.builder()
                .valido(false)
                .integridadValida(false)
                .mensaje("No se encontró ningún documento oficial registrado con dicho código de verificación.")
                .build());
    }
}
