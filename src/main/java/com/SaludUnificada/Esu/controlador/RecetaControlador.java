package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.DispensarRecetaDtoRequest;
import com.SaludUnificada.Esu.dto.request.RecetaDigitalDtoRequest;
import com.SaludUnificada.Esu.dto.response.DispensarRecetaDtoResponse;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.servicio.RecetaDigitalServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Recetas Digitales", description = "Emision, validacion y descarga oficial de recetas medicas digitales")
public class RecetaControlador {

    @Autowired
    private RecetaDigitalServicio recetaServicio;

    private void validarPropiedadReceta(RecetaDigital receta, Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || receta == null) {
            return;
        }
        boolean esPersonal = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_MEDICO") ||
                a.getAuthority().equals("ROLE_PROFESIONAL"));
        if (!esPersonal && receta.getPaciente() != null && receta.getPaciente().getUsuario() != null) {
            if (!receta.getPaciente().getUsuario().getEmail().equalsIgnoreCase(auth.getName())) {
                throw new AccessDeniedException("No posee autorizacion para acceder a recetas de otro paciente");
            }
        }
    }

    @PostMapping("/recetas")
    @Operation(summary = "Emitir nueva receta medica digital")
    public ResponseEntity<RecetaDigital> emitirReceta(@Valid @RequestBody RecetaDigitalDtoRequest request) {
        return new ResponseEntity<>(recetaServicio.emitirReceta(request), HttpStatus.CREATED);
    }

    @PostMapping("/recetas/validar")
    @Operation(summary = "Validar requisitos previos para prescripcion medica")
    public ResponseEntity<Map<String, Object>> validarPrescripcion(@Valid @RequestBody RecetaDigitalDtoRequest request) {
        recetaServicio.validarProfesionalHabilitado(request.getProfesionalId());
        return ResponseEntity.ok(Map.of(
            "valido", true,
            "mensaje", "Profesional habilitado y prescripcion valida con formato oficial."
        ));
    }

    @PostMapping("/recetas/dispensar")
    @Operation(summary = "Endpoint transaccional de dispensacion farmaceutica de recetas medicas")
    public ResponseEntity<DispensarRecetaDtoResponse> dispensarReceta(@Valid @RequestBody DispensarRecetaDtoRequest request) {
        return ResponseEntity.ok(recetaServicio.dispensarReceta(request));
    }

    @GetMapping("/recetas/{id}")
    @Operation(summary = "Obtener datos de una receta medica")
    public ResponseEntity<RecetaDigital> obtenerReceta(@PathVariable Long id, Authentication authentication) {
        RecetaDigital receta = recetaServicio.obtenerPorId(id);
        validarPropiedadReceta(receta, authentication);
        return ResponseEntity.ok(receta);
    }

    @GetMapping("/recetas/{id}/pdf")
    @Operation(summary = "Descargar receta medica oficial en PDF con codigo QR y Hash SHA-256")
    public ResponseEntity<byte[]> descargarRecetaPdf(@PathVariable Long id, Authentication authentication) {
        RecetaDigital receta = recetaServicio.obtenerPorId(id);
        validarPropiedadReceta(receta, authentication);
        byte[] pdfBytes = recetaServicio.generarPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receta_digital_" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/public/verificar/{hash}")
    @Operation(summary = "Endpoint publico para verificar autenticidad e integridad mediante QR o Hash")
    public ResponseEntity<Map<String, Object>> verificarDocumentoPublico(@PathVariable String hash) {
        return ResponseEntity.ok(recetaServicio.verificarDocumentoPublico(hash));
    }
    @GetMapping("/recetas/paciente/{pacienteId}")
    @Operation(summary = "Listar recetas medicas digitales por paciente")
    public ResponseEntity<List<RecetaDigital>> listarPorPaciente(
            @PathVariable Long pacienteId,
            @RequestParam(required = false) String estado,
            Authentication authentication) {
        return ResponseEntity.ok(recetaServicio.listarPorPaciente(pacienteId, estado));
    }

    @PatchMapping("/recetas/{id}/anular")
    public ResponseEntity<RecetaDigital> anularReceta(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload,
            Principal principal) {
        String motivo = payload != null ? payload.get("motivo") : null;
        return ResponseEntity.ok(recetaServicio.anularReceta(id, motivo, principal != null ? principal.getName() : null));
    }
}
