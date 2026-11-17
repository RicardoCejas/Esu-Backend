package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.FamiliarDtoRequest;
import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.CarnetDigitalDtoResponse;
import com.SaludUnificada.Esu.dto.response.ConvenioObraSocialDtoResponse;
import com.SaludUnificada.Esu.dto.response.FamiliarDtoResponse;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.servicio.CarnetYFamiliarServicio;
import com.SaludUnificada.Esu.servicio.IPacienteServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Gestión del padrón de pacientes, búsqueda por DNI, grupo familiar y carnet digital")
public class PacienteControlador {

    private final IPacienteServicio pacienteServicio;
    private final CarnetYFamiliarServicio carnetYFamiliarServicio;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Obtener ficha del paciente autenticado")
    public ResponseEntity<PacienteDtoResponse> obtenerMiPerfil(Principal principal) {
        return ResponseEntity.ok(pacienteServicio.obtenerPerfilAutenticado(principal.getName()));
    }

    @PostMapping({"", "/crear"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL')")
    @Operation(summary = "Dar de alta nuevo paciente en el padrón")
    public ResponseEntity<PacienteDtoResponse> crearPaciente(@Valid @RequestBody PacienteDtoRequest pacienteDto) {
        PacienteDtoResponse nuevoPaciente = pacienteServicio.crearPaciente(pacienteDto);
        return new ResponseEntity<>(nuevoPaciente, HttpStatus.CREATED);
    }

    @GetMapping({"/{id:\\d+}", "/obtener/{id:\\d+}"})
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    @Operation(summary = "Obtener paciente por ID")
    public ResponseEntity<PacienteDtoResponse> obtenerPacientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteServicio.obtenerPacientePorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL')")
    @Operation(summary = "Listar pacientes del padrón")
    public ResponseEntity<List<PacienteDtoResponse>> listarTodos() {
        return ResponseEntity.ok(pacienteServicio.listarTodos());
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL')")
    @Operation(summary = "Listar pacientes paginados")
    public ResponseEntity<Page<PacienteDtoResponse>> listarPaginado(Pageable pageable) {
        return ResponseEntity.ok(pacienteServicio.buscarPacientesPaginados(pageable));
    }

    @GetMapping("/conteo")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'PROFESIONAL', 'RECEPCIONISTA')")
    @Operation(summary = "Contar total de pacientes registrados")
    public ResponseEntity<Long> contarPacientes() {
        return ResponseEntity.ok(pacienteServicio.contarPacientes());
    }

    @DeleteMapping({"/{id:\\d+}", "/eliminar/{id:\\d+}"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar paciente")
    public ResponseEntity<Void> eliminarPaciente(@PathVariable Long id) {
        pacienteServicio.eliminarPaciente(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar/dni")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'PROFESIONAL', 'RECEPCIONISTA')")
    @Operation(summary = "Buscar paciente por DNI (query param)")
    public ResponseEntity<PacienteDtoResponse> buscarPorDni(@RequestParam String dni) {
        return ResponseEntity.ok(pacienteServicio.buscarPorDni(dni));
    }

    @GetMapping("/dni/{dni}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'PROFESIONAL', 'RECEPCIONISTA')")
    @Operation(summary = "Buscar paciente por DNI (path variable)")
    public ResponseEntity<PacienteDtoResponse> buscarPorDniPath(@PathVariable String dni) {
        return ResponseEntity.ok(pacienteServicio.buscarPorDni(dni));
    }

    @GetMapping("/buscar/apellido")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'PROFESIONAL', 'RECEPCIONISTA')")
    @Operation(summary = "Buscar pacientes por coincidencia de apellido")
    public ResponseEntity<List<PacienteDtoResponse>> buscarPorApellidoParcial(@RequestParam String apellido) {
        return ResponseEntity.ok(pacienteServicio.buscarPorApellidoParcial(apellido));
    }

    @GetMapping("/{id:\\d+}/carnet-digital")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    @Operation(summary = "Emitir carnet digital con firma HMAC")
    public ResponseEntity<CarnetDigitalDtoResponse> emitirCarnetDigital(@PathVariable Long id) {
        return ResponseEntity.ok(carnetYFamiliarServicio.emitirCarnetDigital(id));
    }

    @PostMapping("/verificar-carnet")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Verificar validez criptográfica de un carnet digital")
    public ResponseEntity<Boolean> verificarCarnet(@RequestParam String qrPayload) {
        return ResponseEntity.ok(carnetYFamiliarServicio.verificarCarnet(qrPayload));
    }

    @PostMapping("/{id:\\d+}/familiares")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    @Operation(summary = "Vincular integrante al grupo familiar del paciente")
    public ResponseEntity<FamiliarDtoResponse> vincularFamiliar(
            @PathVariable Long id,
            @Valid @RequestBody FamiliarDtoRequest request) {
        return new ResponseEntity<>(carnetYFamiliarServicio.vincularFamiliar(id, request), HttpStatus.CREATED);
    }

    @GetMapping("/{id:\\d+}/familiares")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    @Operation(summary = "Listar grupo familiar del paciente")
    public ResponseEntity<List<FamiliarDtoResponse>> listarFamiliares(@PathVariable Long id) {
        return ResponseEntity.ok(carnetYFamiliarServicio.listarFamiliares(id));
    }

    @DeleteMapping("/{titularId:\\d+}/familiares/{vinculoId:\\d+}")
    @PreAuthorize("@securityPolicy.canAccessPatient(#titularId) and @securityPolicy.canAccessFamiliar(#vinculoId)")
    @Operation(summary = "Desvincular integrante del grupo familiar")
    public ResponseEntity<Void> desvincularFamiliar(@PathVariable Long titularId, @PathVariable Long vinculoId) {
        carnetYFamiliarServicio.desvincularFamiliar(titularId, vinculoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/convenios")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar convenios de obras sociales activos")
    public ResponseEntity<List<ConvenioObraSocialDtoResponse>> listarConveniosActivos() {
        var convenios = carnetYFamiliarServicio.listarConveniosActivos();
        var dtos = convenios.stream().map(c -> ConvenioObraSocialDtoResponse.builder()
                .id(c.getId())
                .nombreObraSocial(c.getNombreObraSocial())
                .sigla(c.getSigla())
                .codigoRnosc(c.getCodigoRnosc())
                .patronRegexAfiliado(c.getPatronRegexAfiliado())
                .requiereCoseguro(c.getRequiereCoseguro())
                .activo(c.getActivo())
                .build()).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping({"/{id:\\d+}", "/actualizar/{id:\\d+}"})
    @PreAuthorize("@securityPolicy.canAccessPatientAdmin(#id)")
    @Operation(summary = "Actualizar información administrativa completa del paciente")
    public ResponseEntity<PacienteDtoResponse> actualizarPaciente(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(pacienteServicio.actualizarPacienteCompleto(id, body));
    }

    @PutMapping("/{id:\\d+}/contacto")
    @PreAuthorize("@securityPolicy.canAccessPatientAdmin(#id)")
    @Operation(summary = "Actualizar datos de contacto y cobertura del paciente")
    public ResponseEntity<PacienteDtoResponse> actualizarContacto(
            @PathVariable Long id,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String obraSocial,
            @RequestParam(required = false) String nroAfiliado,
            @RequestParam(required = false) String dni,
            @RequestBody(required = false) Map<String, String> body) {
        String tel = (telefono != null) ? telefono : (body != null ? body.get("telefono") : null);
        String os = (obraSocial != null) ? obraSocial : (body != null ? body.get("obraSocial") : null);
        String af = (nroAfiliado != null) ? nroAfiliado : (body != null ? body.getOrDefault("nroAfiliado", body.get("numeroAfiliado")) : null);
        String doc = (dni != null) ? dni : (body != null ? body.get("dni") : null);
        return ResponseEntity.ok(pacienteServicio.actualizarDatosContacto(id, tel, os, af, doc));
    }

    @PutMapping("/mi-perfil")
    @PreAuthorize("hasAnyRole('PACIENTE', 'ADMIN', 'DEV')")
    @Operation(summary = "Actualizar el perfil y DNI del paciente autenticado")
    public ResponseEntity<PacienteDtoResponse> actualizarMiPerfil(
            @RequestBody Map<String, String> body,
            Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(pacienteServicio.actualizarMiPerfilPorEmail(principal.getName(), body));
    }

    @PostMapping("/fusionar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fusionar registros duplicados de pacientes con auditoría")
    public ResponseEntity<Map<String, String>> fusionarPacientes(
            @RequestParam Long pacienteOrigenId,
            @RequestParam Long pacienteDestinoId,
            @RequestParam(required = false) String motivo,
            Principal principal) {
        pacienteServicio.fusionarPacientes(pacienteOrigenId, pacienteDestinoId, motivo, principal != null ? principal.getName() : "ADMIN");
        return ResponseEntity.ok(Map.of("mensaje", "Pacientes fusionados correctamente"));
    }
}
