package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.FamiliarDtoRequest;
import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.CarnetDigitalDtoResponse;
import com.SaludUnificada.Esu.dto.response.FamiliarDtoResponse;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.servicio.CarnetYFamiliarServicio;
import com.SaludUnificada.Esu.servicio.IPacienteServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteControlador {

    @Autowired
    private IPacienteServicio pacienteServicio;

    @Autowired
    private CarnetYFamiliarServicio carnetYFamiliarServicio;

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
                throw new AccessDeniedException("No posee autorizacion para acceder a los datos de otro paciente");
            }
        }
    }

    @GetMapping("/me")
    public ResponseEntity<PacienteDtoResponse> obtenerMiPerfil(Principal principal) {
        return ResponseEntity.ok(pacienteServicio.obtenerPerfilAutenticado(principal.getName()));
    }

    @PostMapping("/crear")
    public ResponseEntity<PacienteDtoResponse> crearPaciente(@Valid @RequestBody PacienteDtoRequest pacienteDto) {
        PacienteDtoResponse nuevoPaciente = pacienteServicio.crearPaciente(pacienteDto);
        return new ResponseEntity<>(nuevoPaciente, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<PacienteDtoResponse> obtenerPacientePorId(@PathVariable Long id, Authentication authentication) {
        validarPropiedadPaciente(id, authentication);
        return ResponseEntity.ok(pacienteServicio.obtenerPacientePorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    public ResponseEntity<List<PacienteDtoResponse>> listarTodos() {
        return ResponseEntity.ok(pacienteServicio.listarTodos());
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarPaciente(@PathVariable Long id) {
        pacienteServicio.eliminarPaciente(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar/dni")
    public ResponseEntity<PacienteDtoResponse> buscarPorDni(@RequestParam String dni) {
        return ResponseEntity.ok(pacienteServicio.buscarPorDni(dni));
    }

    @GetMapping("/buscar/apellido")
    public ResponseEntity<List<PacienteDtoResponse>> buscarPorApellidoParcial(@RequestParam String apellido) {
        return ResponseEntity.ok(pacienteServicio.buscarPorApellidoParcial(apellido));
    }

    @GetMapping("/{id}/carnet-digital")
    public ResponseEntity<CarnetDigitalDtoResponse> emitirCarnetDigital(@PathVariable Long id, Authentication authentication) {
        validarPropiedadPaciente(id, authentication);
        return ResponseEntity.ok(carnetYFamiliarServicio.emitirCarnetDigital(id));
    }

    @PostMapping("/verificar-carnet")
    public ResponseEntity<Boolean> verificarCarnet(@RequestParam String qrPayload) {
        return ResponseEntity.ok(carnetYFamiliarServicio.verificarCarnet(qrPayload));
    }

    @PostMapping("/{id}/familiares")
    public ResponseEntity<FamiliarDtoResponse> vincularFamiliar(
            @PathVariable Long id,
            @Valid @RequestBody FamiliarDtoRequest request,
            Authentication authentication) {
        validarPropiedadPaciente(id, authentication);
        return new ResponseEntity<>(carnetYFamiliarServicio.vincularFamiliar(id, request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/familiares")
    public ResponseEntity<List<FamiliarDtoResponse>> listarFamiliares(@PathVariable Long id, Authentication authentication) {
        validarPropiedadPaciente(id, authentication);
        return ResponseEntity.ok(carnetYFamiliarServicio.listarFamiliares(id));
    }

    @DeleteMapping("/{titularId}/familiares/{vinculoId}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Void> desvincularFamiliar(@PathVariable Long titularId, @PathVariable Long vinculoId) {
        carnetYFamiliarServicio.desvincularFamiliar(titularId, vinculoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/convenios")
    public ResponseEntity<List<com.SaludUnificada.Esu.entidad.ConvenioObraSocial>> listarConveniosActivos() {
        return ResponseEntity.ok(carnetYFamiliarServicio.listarConveniosActivos());
    }

    @PutMapping("/{id}/contacto")
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<PacienteDtoResponse> actualizarContacto(
            @PathVariable Long id,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String obraSocial,
            @RequestParam(required = false) String nroAfiliado,
            Authentication authentication) {
        validarPropiedadPaciente(id, authentication);
        return ResponseEntity.ok(pacienteServicio.actualizarDatosContacto(id, telefono, obraSocial, nroAfiliado));
    }
}
