package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.EstudioAdjuntoDtoResponse;
import com.SaludUnificada.Esu.entidad.EstudioAdjunto;
import com.SaludUnificada.Esu.servicio.EstudioAdjuntoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/estudios")
@RequiredArgsConstructor
@Tag(name = "Estudios Médicos Adjuntos", description = "Almacenamiento seguro, hash SHA-256 y descarga de estudios complementarios (HU-101 / HU-73)")
public class EstudioAdjuntoControlador {

    private final EstudioAdjuntoServicio estudioServicio;

    @PostMapping(value = "/subir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Subir informe de laboratorio, radiografía o estudio complementario (PDF/JPG/PNG max 15MB)")
    public ResponseEntity<EstudioAdjuntoDtoResponse> subirEstudio(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("pacienteId") Long pacienteId,
            @RequestParam(value = "evolucionId", required = false) Long evolucionId,
            @RequestParam(value = "tipoEstudio", defaultValue = "INFORME") String tipoEstudio,
            @RequestParam(value = "fechaEstudio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEstudio
    ) {
        EstudioAdjuntoDtoResponse response = estudioServicio.almacenarEstudio(
                archivo, pacienteId, evolucionId, tipoEstudio, fechaEstudio
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/descargar")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Descarga segura del archivo físico binario del estudio médico")
    public ResponseEntity<Resource> descargarEstudio(@PathVariable Long id) {
        EstudioAdjunto estudio = estudioServicio.obtenerPorId(id);
        Resource resource = estudioServicio.descargarArchivo(id);

        String contentType = estudio.getMimeType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + estudio.getNombreArchivo() + "\"")
                .body(resource);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Listar todos los estudios médicos complementarios de un paciente")
    public ResponseEntity<List<EstudioAdjuntoDtoResponse>> listarPorPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(estudioServicio.listarPorPaciente(pacienteId));
    }

    @GetMapping("/evolucion/{evolucionId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Listar los estudios médicos adjuntos a una consulta clínica específica")
    public ResponseEntity<List<EstudioAdjuntoDtoResponse>> listarPorEvolucion(@PathVariable Long evolucionId) {
        return ResponseEntity.ok(estudioServicio.listarPorEvolucion(evolucionId));
    }
}
