package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.ConsultaBorradorDtoRequest;
import com.SaludUnificada.Esu.dto.request.ConsultaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.request.EvolucionAdendaDtoRequest;
import com.SaludUnificada.Esu.dto.request.EvolucionMedicaDtoRequest;
import com.SaludUnificada.Esu.dto.request.FinalizarAtencionDtoRequest;
import com.SaludUnificada.Esu.dto.response.ConsultaBorradorDtoResponse;
import com.SaludUnificada.Esu.dto.response.EvolucionAdendaDtoResponse;
import com.SaludUnificada.Esu.dto.response.EvolucionMedicaDtoResponse;
import com.SaludUnificada.Esu.dto.response.ResumenClinicoDtoResponse;
import com.SaludUnificada.Esu.dto.response.TimelineItemDto;
import com.SaludUnificada.Esu.mapper.ClinicaMapper;
import com.SaludUnificada.Esu.seguridad.AutorizacionServicio;
import com.SaludUnificada.Esu.servicio.ConsultaBorradorServicio;
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

@RestController
@RequestMapping("/api")
@Tag(name = "Evoluciones Clínicas", description = "Gestión inalterable de evoluciones, adendas, resumen clínico y cierre de atención")
public class EvolucionControlador {

    @Autowired
    private EvolucionMedicaServicio evolucionServicio;

    @Autowired
    private ConsultaBorradorServicio consultaBorradorServicio;

    @Autowired
    private AutorizacionServicio autorizacionServicio;

    @Autowired
    private ClinicaMapper clinicaMapper;

    @PostMapping("/historias-clinicas/evoluciones")
    @Operation(summary = "Registrar y firmar electrónicamente una evolución médica clínica")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<EvolucionMedicaDtoResponse> registrarEvolucion(@Valid @RequestBody EvolucionMedicaDtoRequest request) {
        return new ResponseEntity<>(
                clinicaMapper.paraDto(evolucionServicio.registrarEvolucion(request)),
                HttpStatus.CREATED);
    }

    @PostMapping("/historias-clinicas/evoluciones/{id}/adenda")
    @Operation(summary = "Emitir Adenda médica / Fe de erratas formal sobre una evolución firmada")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<EvolucionAdendaDtoResponse> emitirAdenda(
        @PathVariable Long id,
        @Valid @RequestBody EvolucionAdendaDtoRequest request
    ) {
        return new ResponseEntity<>(
                clinicaMapper.paraDto(evolucionServicio.registrarAdenda(id, request)),
                HttpStatus.CREATED);
    }

    @PutMapping("/turnos/{id}/finalizar-atencion")
    @Operation(summary = "Finalizar atención médica, asociar evolución y cerrar turno a FINALIZADO")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<EvolucionMedicaDtoResponse> finalizarAtencion(
        @PathVariable Long id,
        @Valid @RequestBody FinalizarAtencionDtoRequest request
    ) {
        return ResponseEntity.ok(clinicaMapper.paraDto(evolucionServicio.finalizarAtencion(id, request)));
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
    @Operation(summary = "Guardar borrador de consulta medica")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<ConsultaBorradorDtoResponse> guardarBorrador(
            @Valid @RequestBody ConsultaBorradorDtoRequest request) {
        autorizacionServicio.verificarAccesoAPaciente(request.getPacienteId());
        return ResponseEntity.ok(consultaBorradorServicio.guardar(request));
    }

    @PostMapping("/historias-clinicas/consultas")
    @Operation(summary = "Registrar una consulta medica desde la atencion asistencial")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<EvolucionMedicaDtoResponse> registrarConsulta(
            @RequestBody ConsultaClinicaDtoRequest request) {
        EvolucionMedicaDtoRequest evolucion = new EvolucionMedicaDtoRequest();
        evolucion.setPacienteId(parsearId(request.getPacienteId(), "paciente"));
        evolucion.setProfesionalId(parsearId(request.getProfesionalId(), "profesional"));
        evolucion.setTurnoId(parsearId(request.getTurnoId(), "turno"));
        evolucion.setMotivoConsulta(textoONulo(request.getMotivo(), "Consulta medica"));
        evolucion.setDiagnostico(textoONulo(request.getDiagnostico(), "Consulta medica de rutina"));
        evolucion.setTratamiento(request.getTratamiento());
        evolucion.setFirmar(true);

        if (request.getSignosVitales() != null) {
            evolucion.setPresionArterial(request.getSignosVitales().get("presionArterial"));
            evolucion.setFrecuenciaCardiaca(request.getSignosVitales().get("frecuenciaCardiaca"));
            evolucion.setTemperatura(request.getSignosVitales().get("temperatura"));
            evolucion.setPeso(request.getSignosVitales().get("peso"));
            evolucion.setTalla(request.getSignosVitales().get("talla"));
            evolucion.setImc(request.getSignosVitales().get("imc"));
        }

        return new ResponseEntity<>(
                clinicaMapper.paraDto(evolucionServicio.registrarEvolucion(evolucion)),
                HttpStatus.CREATED);
    }

    @GetMapping("/historias-clinicas/consultas/borrador")
    @Operation(summary = "Recuperar el ultimo borrador de consulta del paciente")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN','PACIENTE')")
    public ResponseEntity<ConsultaBorradorDtoResponse> obtenerBorrador(
            @RequestParam Long pacienteId,
            @RequestParam Long profesionalId) {
        autorizacionServicio.verificarAccesoAPaciente(pacienteId);
        return ResponseEntity.ok(consultaBorradorServicio.obtener(pacienteId, profesionalId));
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

    private Long parsearId(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El ID de " + campo + " no es valido: " + valor);
        }
    }

    private String textoONulo(String valor, String porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }
}
