package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.ConsultaBorradorDtoRequest;
import com.SaludUnificada.Esu.dto.request.EvolucionAdendaDtoRequest;
import com.SaludUnificada.Esu.dto.request.EvolucionMedicaDtoRequest;
import com.SaludUnificada.Esu.dto.request.FinalizarAtencionDtoRequest;
import com.SaludUnificada.Esu.dto.response.ConsultaBorradorDtoResponse;
import com.SaludUnificada.Esu.dto.response.EvolucionAdendaDtoResponse;
import com.SaludUnificada.Esu.dto.response.EvolucionMedicaDtoResponse;
import com.SaludUnificada.Esu.dto.response.ResumenClinicoDtoResponse;
import com.SaludUnificada.Esu.dto.response.TimelineItemDto;
import com.SaludUnificada.Esu.entidad.ConsultaBorrador;
import com.SaludUnificada.Esu.entidad.EvolucionAdenda;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import com.SaludUnificada.Esu.mapper.EvolucionMedicaMapper;
import com.SaludUnificada.Esu.servicio.EvolucionMedicaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Evoluciones ClÃ­nicas", description = "GestiÃ³n inalterable de evoluciones, adendas, resumen clÃ­nico y cierre de atenciÃ³n")
public class EvolucionControlador {

    private final EvolucionMedicaServicio evolucionServicio;
    private final EvolucionMedicaMapper evolucionMedicaMapper;

    @PostMapping("/historias-clinicas/evoluciones")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL') and @securityPolicy.canAccessPatient(#request.pacienteId)")
    @Operation(summary = "Registrar y firmar electrónicamente una evolución médica clínica")
    public ResponseEntity<EvolucionMedicaDtoResponse> registrarEvolucion(@Valid @RequestBody EvolucionMedicaDtoRequest request) {
        EvolucionMedica entidad = evolucionServicio.registrarEvolucion(request);
        return new ResponseEntity<>(evolucionMedicaMapper.toDto(entidad), HttpStatus.CREATED);
    }

    @PostMapping("/historias-clinicas/evoluciones/{id}/adenda")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL') and @securityPolicy.canAccessEvolucion(#id)")
    @Operation(summary = "Emitir Adenda mÃ©dica / Fe de erratas formal sobre una evoluciÃ³n firmada")
    public ResponseEntity<EvolucionAdendaDtoResponse> emitirAdenda(
        @PathVariable Long id,
        @Valid @RequestBody EvolucionAdendaDtoRequest request
    ) {
        EvolucionAdenda entidad = evolucionServicio.registrarAdenda(id, request);
        return new ResponseEntity<>(evolucionMedicaMapper.toAdendaDto(entidad), HttpStatus.CREATED);
    }

    @PutMapping("/turnos/{id}/finalizar-atencion")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL') and @securityPolicy.isTurnoDoctor(#id)")
    @Operation(summary = "Finalizar atenciÃ³n mÃ©dica, asociar evoluciÃ³n y cerrar turno a FINALIZADO")
    public ResponseEntity<EvolucionMedicaDtoResponse> finalizarAtencion(
        @PathVariable Long id,
        @Valid @RequestBody FinalizarAtencionDtoRequest request
    ) {
        EvolucionMedica entidad = evolucionServicio.finalizarAtencion(id, request);
        return ResponseEntity.ok(evolucionMedicaMapper.toDto(entidad));
    }

    @GetMapping("/pacientes/{id}/resumen-clinico")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    @Operation(summary = "Obtener resumen clÃ­nico, panel de alertas mÃ©dicas crÃ­ticas fijas y signos vitales")
    public ResponseEntity<ResumenClinicoDtoResponse> obtenerResumenClinico(@PathVariable Long id) {
        return ResponseEntity.ok(evolucionServicio.obtenerResumenClinico(id));
    }

    @GetMapping("/historias-clinicas/paciente/{id}/timeline")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    @Operation(summary = "Obtener lÃ­nea de tiempo cronolÃ³gica completa de atenciones del paciente")
    public ResponseEntity<List<TimelineItemDto>> obtenerTimeline(@PathVariable Long id) {
        return ResponseEntity.ok(evolucionServicio.obtenerTimelinePaciente(id));
    }

    @GetMapping("/historias-clinicas/{pacienteId}/timeline")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    @Operation(summary = "BÃºsqueda parametrizada y paginaciÃ³n optimizada del timeline clÃ­nico (HU-102)")
    public ResponseEntity<Page<TimelineItemDto>> obtenerTimelineParametrizado(
            @PathVariable Long pacienteId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cie10,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(evolucionServicio.obtenerTimelineParametrizado(pacienteId, q, cie10, desde, hasta, pageable));
    }

    @PostMapping("/historias-clinicas/consultas/borrador")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL')")
    @Operation(summary = "Guardar borrador de consulta mÃ©dica")
    public ResponseEntity<ConsultaBorradorDtoResponse> guardarBorrador(
            @RequestBody ConsultaBorradorDtoRequest borrador,
            Principal principal) {
        return ResponseEntity.ok(evolucionServicio.guardarBorradorConsulta(borrador, principal != null ? principal.getName() : null));
    }

    @GetMapping("/historias-clinicas/consultas/borrador")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL')")
    @Operation(summary = "Consultar borrador de consulta mÃ©dica")
    public ResponseEntity<ConsultaBorrador> obtenerBorrador(
            @RequestParam(required = false) Long turnoId,
            @RequestParam(required = false) Long pacienteId,
            Principal principal) {
        ConsultaBorrador borrador = evolucionServicio.obtenerBorradorConsulta(turnoId, pacienteId, principal != null ? principal.getName() : null);
        if (borrador == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(borrador);
    }

    @GetMapping("/historias-clinicas/evoluciones/{id}/pdf")
    @PreAuthorize("@securityPolicy.canAccessEvolucion(#id)")
    @Operation(summary = "Descargar informe de evoluciÃ³n mÃ©dica inalterable en PDF con QR y SHA-256")
    public ResponseEntity<StreamingResponseBody> descargarEvolucionPdf(@PathVariable Long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "evolucion_clinica_" + id + ".pdf");

        StreamingResponseBody stream = outputStream -> {
            byte[] pdfBytes = evolucionServicio.generarPdfEvolucion(id);
            outputStream.write(pdfBytes);
            outputStream.flush();
        };

        return new ResponseEntity<>(stream, headers, HttpStatus.OK);
    }
}
