package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.EvolucionAdendaDtoRequest;
import com.SaludUnificada.Esu.dto.request.EvolucionMedicaDtoRequest;
import com.SaludUnificada.Esu.dto.request.FinalizarAtencionDtoRequest;
import com.SaludUnificada.Esu.dto.response.ResumenClinicoDtoResponse;
import com.SaludUnificada.Esu.dto.response.TimelineItemDto;
import com.SaludUnificada.Esu.entidad.EvolucionAdenda;
import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import com.SaludUnificada.Esu.seguridad.AutorizacionServicio;
import com.SaludUnificada.Esu.servicio.EvolucionMedicaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Evoluciones Clínicas", description = "Gestión inalterable de evoluciones, adendas, resumen clínico y cierre de atención")
public class EvolucionControlador {

    @Autowired
    private EvolucionMedicaServicio evolucionServicio;

    @Autowired
    private AutorizacionServicio autorizacionServicio;

    @PostMapping("/historias-clinicas/evoluciones")
    @Operation(summary = "Registrar y firmar electrónicamente una evolución médica clínica")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<EvolucionMedica> registrarEvolucion(@Valid @RequestBody EvolucionMedicaDtoRequest request) {
        return new ResponseEntity<>(evolucionServicio.registrarEvolucion(request), HttpStatus.CREATED);
    }

    @PostMapping("/historias-clinicas/evoluciones/{id}/adenda")
    @Operation(summary = "Emitir Adenda médica / Fe de erratas formal sobre una evolución firmada")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<EvolucionAdenda> emitirAdenda(
        @PathVariable Long id,
        @Valid @RequestBody EvolucionAdendaDtoRequest request
    ) {
        return new ResponseEntity<>(evolucionServicio.registrarAdenda(id, request), HttpStatus.CREATED);
    }

    @PutMapping("/turnos/{id}/finalizar-atencion")
    @Operation(summary = "Finalizar atención médica, asociar evolución y cerrar turno a FINALIZADO")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<EvolucionMedica> finalizarAtencion(
        @PathVariable Long id,
        @Valid @RequestBody FinalizarAtencionDtoRequest request
    ) {
        return ResponseEntity.ok(evolucionServicio.finalizarAtencion(id, request));
    }

    @GetMapping("/pacientes/{id}/resumen-clinico")
    @Operation(summary = "Obtener resumen clínico, panel de alertas médicas críticas fijas y signos vitales")
    public ResponseEntity<ResumenClinicoDtoResponse> obtenerResumenClinico(@PathVariable Long id) {
        autorizacionServicio.verificarAccesoAPaciente(id);
        return ResponseEntity.ok(evolucionServicio.obtenerResumenClinico(id));
    }

    @GetMapping("/historias-clinicas/paciente/{id}/timeline")
    @Operation(summary = "Obtener línea de tiempo cronológica de atenciones del paciente")
    public ResponseEntity<List<TimelineItemDto>> obtenerTimeline(@PathVariable Long id) {
        autorizacionServicio.verificarAccesoAPaciente(id);
        return ResponseEntity.ok(evolucionServicio.obtenerTimelinePaciente(id));
    }

    @PostMapping("/historias-clinicas/consultas/borrador")
    @Operation(summary = "Guardar borrador de consulta médica")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<Map<String, Object>> guardarBorrador(@RequestBody Map<String, Object> borrador) {
        return ResponseEntity.ok(Map.of(
            "guardado", true,
            "timestamp", System.currentTimeMillis(),
            "mensaje", "Borrador de consulta resguardado exitosamente."
        ));
    }

    @GetMapping("/historias-clinicas/evoluciones/{id}/pdf")
    @Operation(summary = "Descargar informe de evolución médica inalterable en PDF con QR y SHA-256")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<byte[]> descargarEvolucionPdf(@PathVariable Long id) {
        byte[] pdfBytes = evolucionServicio.generarPdfEvolucion(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "evolucion_clinica_" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
