package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.AtencionTurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.CopagoDtoRequest;
import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoMostradorDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoPublicoDtoRequest;
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
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@Tag(name = "Turnos", description = "Gestion de turnos medicos, check-in, copagos, atencion y demanda espontanea")
public class TurnoControlador {

    private final ITurnoServicio turnoServicio;
    private final RecordatorioTurnoServicio recordatorioTurnoServicio;
    @Autowired(required = false)
    private com.SaludUnificada.Esu.seguridad.PublicBookingRateLimiter publicBookingRateLimiter;

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

    @PostMapping("/mostrador")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isReceptionistOfCenter(#request.centroSaludId)")
    @Operation(summary = "Registrar turno presencial de mostrador en recepcion")
    public ResponseEntity<TurnoDtoResponse> crearTurnoMostrador(@Valid @RequestBody TurnoMostradorDtoRequest request, Principal principal) {
        TurnoDtoResponse nuevoTurno = turnoServicio.crearTurnoMostrador(request, principal != null ? principal.getName() : "RECEPCION");
        return new ResponseEntity<>(nuevoTurno, HttpStatus.CREATED);
    }

    @PostMapping("/crear")
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<TurnoDtoResponse> crearTurno(@Valid @RequestBody TurnoDtoRequest turnoDto, Principal principal) {
        TurnoDtoResponse nuevoTurno = turnoServicio.crearTurno(turnoDto, principal != null ? principal.getName() : null);
        return new ResponseEntity<>(nuevoTurno, HttpStatus.CREATED);
    }

    @PostMapping("/publico/reservar")
    @Operation(summary = "Reserva pública de turnos oficiales con verificación probatoria de identidad del paciente")
    public ResponseEntity<TurnoDtoResponse> reservarTurnoPublico(
            @Valid @RequestBody TurnoPublicoDtoRequest request,
            Principal principal,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        if (publicBookingRateLimiter != null && !publicBookingRateLimiter.permitirReserva(httpRequest)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Ha superado el límite de reservas públicas permitidas por período. Por favor aguarde unos minutos o inicie sesión."
            );
        }

        TurnoDtoResponse nuevoTurno = turnoServicio.reservarTurnoPublico(request, principal != null ? principal.getName() : null);
        return new ResponseEntity<>(nuevoTurno, HttpStatus.CREATED);
    }

    @GetMapping("/disponibles")
    @Operation(summary = "Consultar horarios disponibles reales para un profesional y fecha")
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

    @GetMapping("/centro/{centroId}/hoy")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isReceptionistOfCenter(#centroId)")
    @Operation(summary = "Obtener turnos del día acotados por centro de salud")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerTurnosHoyCentro(
            @PathVariable Long centroId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(turnoServicio.obtenerTurnosHoyCentro(centroId, fecha));
    }

    @GetMapping("/centro/{centroId}/kanban")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isReceptionistOfCenter(#centroId)")
    @Operation(summary = "Obtener turnos activos de sala de espera (Kanban) acotados por centro de salud")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerTurnosKanbanCentro(
            @PathVariable Long centroId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(turnoServicio.obtenerTurnosKanbanCentro(centroId, fecha));
    }

    @GetMapping({"", "/obtener/todos"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TurnoDtoResponse>> listarTodos() {
        return ResponseEntity.ok(turnoServicio.listarTodos());
    }

    @GetMapping("/paginados")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL')")
    public ResponseEntity<Page<TurnoDtoResponse>> listarPaginados(Pageable pageable) {
        return ResponseEntity.ok(turnoServicio.listarPaginados(pageable));
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarTurno(@PathVariable Long id) {
        turnoServicio.eliminarTurno(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securityPolicy.canAccessTurno(#id)")
    public ResponseEntity<TurnoDtoResponse> cancelarTurnoPorDelete(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(turnoServicio.cancelarTurno(id, principal != null ? principal.getName() : null));
    }

    @GetMapping("/filtrar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL')")
    public ResponseEntity<List<TurnoDtoResponse>> filtrarTurnos(
            @RequestParam Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora) {
        return ResponseEntity.ok(turnoServicio.filtrarTurnos(profesionalId, fechaHora));
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("@securityPolicy.canAccessPatientAdmin(#pacienteId)")
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

    @PutMapping("/{id}/cancelar-mostrador")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.canManageTurnoMostrador(#id)")
    @Operation(summary = "Cancelar turno desde mostrador o bloqueo medico con justificativo auditable")
    public ResponseEntity<TurnoDtoResponse> cancelarTurnoMostrador(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload,
            Principal principal) {
        String justificativo = payload != null ? payload.getOrDefault("justificativo", payload.get("motivo")) : null;
        return ResponseEntity.ok(turnoServicio.cancelarTurnoMostrador(id, justificativo, principal != null ? principal.getName() : "RECEPCION"));
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
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.canManageTurnoMostrador(#id)")
    public ResponseEntity<TurnoDtoResponse> realizarCheckIn(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(turnoServicio.realizarCheckIn(id, principal != null ? principal.getName() : "RECEPCION"));
    }

    @PutMapping("/{id}/llamar")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.canManageTurnoMostrador(#id)")
    @Operation(summary = "Llamar paciente en sala de espera a consultorio")
    public ResponseEntity<TurnoDtoResponse> llamarPaciente(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload,
            Principal principal) {
        String consultorio = payload != null ? payload.get("consultorio") : null;
        return ResponseEntity.ok(turnoServicio.llamarPaciente(id, consultorio, principal != null ? principal.getName() : null));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.canManageTurnoMostrador(#id)")
    @Operation(summary = "Actualizar estado operativo de un turno")
    public ResponseEntity<TurnoDtoResponse> actualizarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        String estado = payload != null ? payload.get("estado") : null;
        return ResponseEntity.ok(turnoServicio.actualizarEstadoTurno(id, estado, principal != null ? principal.getName() : null));
    }

    @RequestMapping(value = "/{id}/copago", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.canManageTurnoMostrador(#id)")
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
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.canManageTurnoMostrador(#id)")
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
