package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.AtencionTurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.CopagoDtoRequest;
import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.RecordatorioTurnoDtoResponse;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @Autowired(required = false)
    private PacienteRepositorio pacienteRepositorio;

    private void validarPropiedadPaciente(Long pacienteId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        boolean esPersonal = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_MEDICO") ||
                a.getAuthority().equals("ROLE_PROFESIONAL") ||
                a.getAuthority().equals("ROLE_RECEPCIONISTA"));
        if (!esPersonal && pacienteRepositorio != null) {
            Paciente p = pacienteRepositorio.findByUsuarioEmail(auth.getName()).orElse(null);
            if (p != null && !p.getId().equals(pacienteId)) {
                throw new AccessDeniedException("No posee autorizacion para acceder a los turnos de otro paciente");
            }
        }
    }

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
    public ResponseEntity<TurnoDtoResponse> obtenerTurnoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(turnoServicio.obtenerTurnoPorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<TurnoDtoResponse>> listarTodos() {
        return ResponseEntity.ok(turnoServicio.listarTodos());
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Void> eliminarTurno(@PathVariable Long id) {
        turnoServicio.eliminarTurno(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtrar")
    public ResponseEntity<List<TurnoDtoResponse>> filtrarTurnos(
            @RequestParam Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora) {
        return ResponseEntity.ok(turnoServicio.filtrarTurnos(profesionalId, fechaHora));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerTurnosPorPaciente(
            @PathVariable Long pacienteId,
            Authentication authentication) {
        validarPropiedadPaciente(pacienteId, authentication);
        return ResponseEntity.ok(turnoServicio.obtenerTurnosPorPaciente(pacienteId));
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<TurnoDtoResponse> cancelarTurno(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(turnoServicio.cancelarTurno(id, principal != null ? principal.getName() : null));
    }

    @PutMapping("/{id}/reprogramar")
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<TurnoDtoResponse> atenderTurno(
            @PathVariable Long id,
            @Valid @RequestBody AtencionTurnoDtoRequest atencionDto,
            Principal principal) {
        return ResponseEntity.ok(turnoServicio.atenderTurno(id, atencionDto, principal != null ? principal.getName() : null));
    }

    @Operation(summary = "Descargar certificado oficial de asistencia a consulta medica en PDF")
    @GetMapping("/{id}/certificado-asistencia")
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<byte[]> descargarCertificadoAsistencia(@PathVariable Long id, Principal principal) {
        byte[] pdfBytes = turnoServicio.generarCertificadoAsistencia(id, principal != null ? principal.getName() : null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "certificado_asistencia_turno_" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
