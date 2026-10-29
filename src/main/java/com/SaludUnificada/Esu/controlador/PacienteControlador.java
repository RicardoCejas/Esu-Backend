package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.FamiliarDtoRequest;
import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.CarnetDigitalDtoResponse;
import com.SaludUnificada.Esu.dto.response.ConvenioObraSocialDtoResponse;
import com.SaludUnificada.Esu.dto.response.FamiliarDtoResponse;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.servicio.CarnetYFamiliarServicio;
import com.SaludUnificada.Esu.servicio.IPacienteServicio;
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
public class PacienteControlador {

    private final IPacienteServicio pacienteServicio;
    private final CarnetYFamiliarServicio carnetYFamiliarServicio;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<PacienteDtoResponse> obtenerMiPerfil(Principal principal) {
        return ResponseEntity.ok(pacienteServicio.obtenerPerfilAutenticado(principal.getName()));
    }

    @PostMapping("/crear")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<PacienteDtoResponse> crearPaciente(@Valid @RequestBody PacienteDtoRequest pacienteDto) {
        PacienteDtoResponse nuevoPaciente = pacienteServicio.crearPaciente(pacienteDto);
        return new ResponseEntity<>(nuevoPaciente, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    public ResponseEntity<PacienteDtoResponse> obtenerPacientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteServicio.obtenerPacientePorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<PacienteDtoResponse>> listarTodos() {
        return ResponseEntity.ok(pacienteServicio.listarTodos());
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Page<PacienteDtoResponse>> listarPaginado(Pageable pageable) {
        return ResponseEntity.ok(pacienteServicio.buscarPacientesPaginados(pageable));
    }

    @GetMapping("/conteo")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'PROFESIONAL', 'RECEPCIONISTA')")
    public ResponseEntity<Long> contarPacientes() {
        return ResponseEntity.ok(pacienteServicio.contarPacientes());
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarPaciente(@PathVariable Long id) {
        pacienteServicio.eliminarPaciente(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar/dni")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'PROFESIONAL', 'RECEPCIONISTA')")
    public ResponseEntity<PacienteDtoResponse> buscarPorDni(@RequestParam String dni) {
        return ResponseEntity.ok(pacienteServicio.buscarPorDni(dni));
    }

    @GetMapping("/buscar/apellido")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'PROFESIONAL', 'RECEPCIONISTA')")
    public ResponseEntity<List<PacienteDtoResponse>> buscarPorApellidoParcial(@RequestParam String apellido) {
        return ResponseEntity.ok(pacienteServicio.buscarPorApellidoParcial(apellido));
    }

    @GetMapping("/{id}/carnet-digital")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    public ResponseEntity<CarnetDigitalDtoResponse> emitirCarnetDigital(@PathVariable Long id) {
        return ResponseEntity.ok(carnetYFamiliarServicio.emitirCarnetDigital(id));
    }

    @PostMapping("/verificar-carnet")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<Boolean> verificarCarnet(@RequestParam String qrPayload) {
        return ResponseEntity.ok(carnetYFamiliarServicio.verificarCarnet(qrPayload));
    }

    @PostMapping("/{id}/familiares")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    public ResponseEntity<FamiliarDtoResponse> vincularFamiliar(
            @PathVariable Long id,
            @Valid @RequestBody FamiliarDtoRequest request) {
        return new ResponseEntity<>(carnetYFamiliarServicio.vincularFamiliar(id, request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/familiares")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    public ResponseEntity<List<FamiliarDtoResponse>> listarFamiliares(@PathVariable Long id) {
        return ResponseEntity.ok(carnetYFamiliarServicio.listarFamiliares(id));
    }

    @DeleteMapping("/{titularId}/familiares/{vinculoId}")
    @PreAuthorize("@securityPolicy.canAccessPatient(#titularId) and @securityPolicy.canAccessFamiliar(#vinculoId)")
    public ResponseEntity<Void> desvincularFamiliar(@PathVariable Long titularId, @PathVariable Long vinculoId) {
        carnetYFamiliarServicio.desvincularFamiliar(titularId, vinculoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/convenios")
    @PreAuthorize("isAuthenticated()")
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

    @PutMapping("/{id}/contacto")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    public ResponseEntity<PacienteDtoResponse> actualizarContacto(
            @PathVariable Long id,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String obraSocial,
            @RequestParam(required = false) String nroAfiliado) {
        return ResponseEntity.ok(pacienteServicio.actualizarDatosContacto(id, telefono, obraSocial, nroAfiliado));
    }

    @PostMapping("/fusionar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> fusionarPacientes(
            @RequestParam Long pacienteOrigenId,
            @RequestParam Long pacienteDestinoId,
            @RequestParam(required = false) String motivo,
            Principal principal) {
        pacienteServicio.fusionarPacientes(pacienteOrigenId, pacienteDestinoId, motivo, principal != null ? principal.getName() : "ADMIN");
        return ResponseEntity.ok(Map.of("mensaje", "Pacientes fusionados correctamente"));
    }
}
