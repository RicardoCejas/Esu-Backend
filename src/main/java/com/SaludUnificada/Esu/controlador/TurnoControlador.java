package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.AtencionTurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.CopagoDtoRequest;
import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.RecordatorioTurnoDtoResponse;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.servicio.ITurnoServicio;
import com.SaludUnificada.Esu.servicio.RecordatorioTurnoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@Tag(name = "Turnos", description = "Gestion de turnos medicos, check-in, copagos, atencion y demanda espontanea")
public class TurnoControlador {

    private final ITurnoServicio turnoServicio;
    private final RecordatorioTurnoServicio recordatorioTurnoServicio;

    @Operation(summary = "Registrar sobreturno medico de urgencia en sala")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sobreturno registrado exitosamente",
                    content = @Content(schema = @Schema(implementation = TurnoDtoResponse.class))),
            @ApiResponse(responseCode = "422", description = "Cupo maximo de sobreturnos diarios superado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Profesional o paciente no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: requiere rol MEDICO o RECEPCIONISTA")
    })
    @PostMapping("/sobreturno")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<TurnoDtoResponse> crearSobreturno(@Valid @RequestBody SobreturnoDtoRequest request) {
        TurnoDtoResponse nuevoTurno = turnoServicio.crearSobreturno(request);
        return new ResponseEntity<>(nuevoTurno, HttpStatus.CREATED);
    }

    @PostMapping("/crear")
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<TurnoDtoResponse> crearTurno(@Valid @RequestBody TurnoDtoRequest turnoDto, Principal principal) {
        TurnoDtoResponse nuevoTurno = turnoServicio.crearTurno(turnoDto, principal != null ? principal.getName() : null);
        return new ResponseEntity<>(nuevoTurno, HttpStatus.CREATED);
    }

    @GetMapping("/disponibles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LocalDateTime>> obtenerDisponibilidad(
            @RequestParam Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(turnoServicio.obtenerHorariosDisponibles(profesionalId, fecha));
    }

    @GetMapping("/recordatorios")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<RecordatorioTurnoDtoResponse>> listarRecordatorios(Principal principal) {
        return ResponseEntity.ok(recordatorioTurnoServicio.listarParaPaciente(principal.getName()));
    }

    @GetMapping("/obtener/{id}")
    @PreAuthorize("@securityPolicy.canAccessTurno(#id)")
    public ResponseEntity<TurnoDtoResponse> obtenerTurnoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(turnoServicio.obtenerTurnoPorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<TurnoDtoResponse>> listarTodos() {
        return ResponseEntity.ok(turnoServicio.listarTodos());
    }

    @GetMapping("/paginados")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Page<TurnoDtoResponse>> listarPaginados(Pageable pageable) {
        return ResponseEntity.ok(turnoServicio.listarPaginados(pageable));
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarTurno(@PathVariable Long id) {
        turnoServicio.eliminarTurno(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtrar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL')")
    public ResponseEntity<List<TurnoDtoResponse>> filtrarTurnos(
            @RequestParam Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora) {
        return ResponseEntity.ok(turnoServicio.filtrarTurnos(profesionalId, fechaHora));
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerTurnosPorPaciente(
            @PathVariable Long pacienteId) {
        return ResponseEntity.ok(turnoServicio.obtenerTurnosPorPaciente(pacienteId));
    }

    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN') or @securityPolicy.isTurnoDoctor(#id)")
    public ResponseEntity<TurnoDtoResponse> confirmarTurno(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(turnoServicio.confirmarTurno(id, principal != null ? principal.getName() : null));
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("@securityPolicy.canAccessTurno(#id)")
    public ResponseEntity<TurnoDtoResponse> cancelarTurno(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(turnoServicio.cancelarTurno(id, principal != null ? principal.getName() : null));
    }

    @PutMapping("/{id}/reprogramar")
    @PreAuthorize("@securityPolicy.canAccessTurno(#id)")
    public ResponseEntity<TurnoDtoResponse> reprogramarTurno(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime nuevaFechaHora,
            Principal principal) {
        return ResponseEntity.ok(turnoServicio.reprogramarTurno(id, nuevaFechaHora, principal != null ? principal.getName() : null));
    }

    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<TurnoDtoResponse> realizarCheckIn(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(turnoServicio.realizarCheckIn(id, principal != null ? principal.getName() : "RECEPCION"));
    }

    @PostMapping("/{id}/copago")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<TurnoDtoResponse> registrarCopago(@PathVariable Long id, @Valid @RequestBody CopagoDtoRequest copagoDto) {
        return ResponseEntity.ok(turnoServicio.registrarCopago(id, copagoDto));
    }

    @PutMapping("/{id}/atender")
    @PreAuthorize("@securityPolicy.isTurnoDoctor(#id)")
    public ResponseEntity<TurnoDtoResponse> atenderTurno(
            @PathVariable Long id,
            @Valid @RequestBody AtencionTurnoDtoRequest atencionDto,
            Principal principal) {
        return ResponseEntity.ok(turnoServicio.atenderTurno(id, atencionDto, principal != null ? principal.getName() : null));
    }

    @Operation(summary = "Descargar certificado oficial de asistencia a consulta medica en PDF")
    @GetMapping("/{id}/certificado-asistencia")
    @PreAuthorize("@securityPolicy.canAccessTurno(#id)")
    public ResponseEntity<byte[]> descargarCertificadoAsistencia(@PathVariable Long id, Principal principal) {
        byte[] pdfBytes = turnoServicio.generarCertificadoAsistencia(id, principal != null ? principal.getName() : null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "certificado_asistencia_turno_" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/paciente/{pacienteId}/proximos")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerTurnosProximos(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(turnoServicio.obtenerTurnosProximosPaciente(pacienteId));
    }

    @GetMapping("/paciente/{pacienteId}/historial")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerHistorialTurnos(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(turnoServicio.obtenerHistorialTurnosPaciente(pacienteId));
    }

    @PatchMapping("/recordatorios/{id}/leido")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<Void> marcarRecordatorioLeido(@PathVariable Long id, Principal principal) {
        recordatorioTurnoServicio.marcarComoLeido(id, principal != null ? principal.getName() : null);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ausente")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN') or @securityPolicy.isTurnoDoctor(#id)")
    public ResponseEntity<TurnoDtoResponse> marcarAusente(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(turnoServicio.marcarAusente(id, principal != null ? principal.getName() : null));
    }

    @GetMapping("/profesional/{profesionalId}/hoy")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN') or @securityPolicy.isDoctorAuthor(#profesionalId)")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerTurnosHoyProfesional(@PathVariable Long profesionalId) {
        return ResponseEntity.ok(turnoServicio.obtenerTurnosHoyProfesional(profesionalId));
    }

    @Operation(summary = "Obtener agenda de turnos de un profesional con filtros opcionales de centro y fecha (HU-BE-89 / Fase 2)")
    @GetMapping("/profesional/{profesionalId}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN') or @securityPolicy.isDoctorAuthor(#profesionalId)")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerAgendaProfesional(
            @PathVariable Long profesionalId,
            @RequestParam(required = false) Long centroSaludId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(turnoServicio.obtenerAgendaProfesional(profesionalId, centroSaludId, fecha));
    }

    @Operation(summary = "Obtener agenda de turnos del médico autenticado filtrada opcionalmente por sede y fecha")
    @GetMapping("/mi-agenda")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL')")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerMiAgenda(
            @RequestParam(required = false) Long centroSaludId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Principal principal) {
        return ResponseEntity.ok(turnoServicio.obtenerMiAgenda(principal != null ? principal.getName() : null, centroSaludId, fecha));
    }
}
