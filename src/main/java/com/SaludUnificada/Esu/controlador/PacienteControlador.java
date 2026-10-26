package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.servicio.IPacienteServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteControlador {

    @Autowired
    private IPacienteServicio pacienteServicio;

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
    public ResponseEntity<PacienteDtoResponse> obtenerPacientePorId(@PathVariable Long id) {
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

    @Autowired
    private com.SaludUnificada.Esu.servicio.CarnetYFamiliarServicio carnetYFamiliarServicio;

    @GetMapping("/buscar/apellido")
    public ResponseEntity<List<PacienteDtoResponse>> buscarPorApellidoParcial(@RequestParam String apellido) {
        return ResponseEntity.ok(pacienteServicio.buscarPorApellidoParcial(apellido));
    }

    // HU-BE-93: Emisión de Carnet Digital con QR y Hash SHA-256
    @GetMapping("/{id}/carnet-digital")
    public ResponseEntity<com.SaludUnificada.Esu.dto.response.CarnetDigitalDtoResponse> emitirCarnetDigital(@PathVariable Long id) {
        return ResponseEntity.ok(carnetYFamiliarServicio.emitirCarnetDigital(id));
    }

    // HU-BE-93: Verificación de QR escaneado
    @PostMapping("/verificar-carnet")
    public ResponseEntity<Boolean> verificarCarnet(@RequestParam String qrPayload) {
        return ResponseEntity.ok(carnetYFamiliarServicio.verificarCarnet(qrPayload));
    }

    // HU-BE-94: Vincular familiar dependiente
    @PostMapping("/{id}/familiares")
    public ResponseEntity<com.SaludUnificada.Esu.dto.response.FamiliarDtoResponse> vincularFamiliar(
            @PathVariable Long id,
            @Valid @RequestBody com.SaludUnificada.Esu.dto.request.FamiliarDtoRequest request) {
        return new ResponseEntity<>(carnetYFamiliarServicio.vincularFamiliar(id, request), HttpStatus.CREATED);
    }

    // HU-BE-94: Listar familiares a cargo
    @GetMapping("/{id}/familiares")
    public ResponseEntity<List<com.SaludUnificada.Esu.dto.response.FamiliarDtoResponse>> listarFamiliares(@PathVariable Long id) {
        return ResponseEntity.ok(carnetYFamiliarServicio.listarFamiliares(id));
    }
}
