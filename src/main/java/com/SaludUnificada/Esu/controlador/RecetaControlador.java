package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.DispensarRecetaDtoRequest;
import com.SaludUnificada.Esu.dto.request.RecetaDigitalDtoRequest;
import com.SaludUnificada.Esu.dto.response.DispensarRecetaDtoResponse;
import com.SaludUnificada.Esu.dto.response.VerificacionRecetaPublicaDtoResponse;
import com.SaludUnificada.Esu.dto.response.RecetaDigitalDtoResponse;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.mapper.RecetaDigitalMapper;
import com.SaludUnificada.Esu.servicio.CatalogoFarmacologicoServicio;
import com.SaludUnificada.Esu.servicio.RecetaDigitalServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Recetas Digitales", description = "Emision, validacion y descarga oficial de recetas medicas digitales")
public class RecetaControlador {

    private final RecetaDigitalServicio recetaServicio;
    private final CatalogoFarmacologicoServicio catalogoFarmacologicoServicio;
    private final RecetaDigitalMapper recetaDigitalMapper;

    @PostMapping("/recetas")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isDoctorAuthor(#request.profesionalId)")
    @Operation(summary = "Emitir nueva receta medica digital")
    public ResponseEntity<RecetaDigitalDtoResponse> emitirReceta(@Valid @RequestBody RecetaDigitalDtoRequest request) {
        RecetaDigital receta = recetaServicio.emitirReceta(request);
        return new ResponseEntity<>(recetaDigitalMapper.toDto(receta), HttpStatus.CREATED);
    }

    @PostMapping("/recetas/validar")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Validar requisitos previos para prescripcion medica")
    public ResponseEntity<Map<String, Object>> validarPrescripcion(@Valid @RequestBody RecetaDigitalDtoRequest request) {
        recetaServicio.validarProfesionalHabilitado(request.getProfesionalId());
        return ResponseEntity.ok(Map.of(
            "valido", true,
            "mensaje", "Profesional habilitado y prescripcion valida con formato oficial."
        ));
    }

    @PostMapping("/recetas/dispensar")
    @PreAuthorize("hasAnyRole('FARMACIA', 'ADMIN')")
    @Operation(summary = "Endpoint transaccional de dispensacion farmaceutica de recetas medicas (Exclusivo Farmacias y Admin)")
    public ResponseEntity<DispensarRecetaDtoResponse> dispensarReceta(@Valid @RequestBody DispensarRecetaDtoRequest request) {
        return ResponseEntity.ok(recetaServicio.dispensarReceta(request));
    }

    @GetMapping("/recetas/{id}")
    @PreAuthorize("@securityPolicy.canAccessReceta(#id)")
    @Operation(summary = "Obtener datos de una receta medica")
    public ResponseEntity<RecetaDigitalDtoResponse> obtenerReceta(@PathVariable Long id) {
        RecetaDigital receta = recetaServicio.obtenerPorId(id);
        return ResponseEntity.ok(recetaDigitalMapper.toDto(receta));
    }

    @GetMapping("/recetas/{id}/pdf")
    @PreAuthorize("@securityPolicy.canAccessReceta(#id)")
    @Operation(summary = "Descargar receta medica oficial en PDF con codigo QR y Hash SHA-256")
    public ResponseEntity<StreamingResponseBody> descargarRecetaPdf(@PathVariable Long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receta_digital_" + id + ".pdf");

        StreamingResponseBody stream = outputStream -> {
            byte[] pdfBytes = recetaServicio.generarPdf(id);
            outputStream.write(pdfBytes);
            outputStream.flush();
        };

        return new ResponseEntity<>(stream, headers, HttpStatus.OK);
    }

    @GetMapping("/public/verificar/{hash}")
    @Operation(summary = "Endpoint publico para verificar autenticidad e integridad mediante QR o Hash")
    public ResponseEntity<VerificacionRecetaPublicaDtoResponse> verificarDocumentoPublico(@PathVariable String hash) {
        return ResponseEntity.ok(recetaServicio.verificarDocumentoPublico(hash));
    }

    @GetMapping("/recetas/paciente/{pacienteId}")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    @Operation(summary = "Listar recetas medicas digitales por paciente")
    public ResponseEntity<List<RecetaDigitalDtoResponse>> listarPorPaciente(
            @PathVariable Long pacienteId,
            @RequestParam(required = false) String estado) {
        List<RecetaDigital> recetas = recetaServicio.listarPorPaciente(pacienteId, estado);
        return ResponseEntity.ok(recetas.stream().map(recetaDigitalMapper::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/recetas/paciente/{pacienteId}/paginado")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    @Operation(summary = "Listar recetas medicas digitales por paciente de forma paginada")
    public ResponseEntity<Page<RecetaDigitalDtoResponse>> listarPorPacientePaginado(
            @PathVariable Long pacienteId,
            Pageable pageable) {
        return ResponseEntity.ok(recetaServicio.listarPorPacientePaginado(pacienteId, pageable).map(recetaDigitalMapper::toDto));
    }

    @PatchMapping("/recetas/{id}/anular")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.canAccessReceta(#id)")
    public ResponseEntity<RecetaDigitalDtoResponse> anularReceta(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload,
            Principal principal) {
        String motivo = payload != null ? payload.get("motivo") : null;
        RecetaDigital receta = recetaServicio.anularReceta(id, motivo, principal != null ? principal.getName() : null);
        return ResponseEntity.ok(recetaDigitalMapper.toDto(receta));
    }

    @GetMapping("/recetas/farmacologia/monodrogas")
    public ResponseEntity<List<String>> listarMonodrogas(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(catalogoFarmacologicoServicio.buscarMonodrogas(query));
    }

    @PostMapping("/recetas/actualizar-vencimientos")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Map<String, Object>> marcarRecetasVencidas() {
        int actualizadas = recetaServicio.marcarRecetasVencidas();
        return ResponseEntity.ok(Map.of("actualizadas", actualizadas));
    }

    @GetMapping("/recetas/cie10/{codigo}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<List<RecetaDigitalDtoResponse>> listarPorCie10(@PathVariable String codigo) {
        List<RecetaDigital> recetas = recetaServicio.listarPorCodigoCie10(codigo);
        return ResponseEntity.ok(recetas.stream().map(recetaDigitalMapper::toDto).collect(Collectors.toList()));
    }
}
