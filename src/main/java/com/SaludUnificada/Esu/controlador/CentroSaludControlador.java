package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.CentroSaludDtoRequest;
import com.SaludUnificada.Esu.dto.request.ConsultorioFisicoDtoRequest;
import com.SaludUnificada.Esu.dto.response.CentroSaludDtoResponse;
import com.SaludUnificada.Esu.dto.response.ConsultorioFisicoDtoResponse;
import com.SaludUnificada.Esu.servicio.CentroSaludServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/centros-salud")
@RequiredArgsConstructor
@Tag(name = "Centros de Salud y Consultorios", description = "Gestión de sedes asistenciales y consultorios físicos (HU-56)")
public class CentroSaludControlador {

    private final CentroSaludServicio centroSaludServicio;

    @GetMapping
    @Operation(summary = "Listar centros de salud activos", description = "Consulta pública del padrón de hospitales, clínicas y dispensarios.")
    public ResponseEntity<List<CentroSaludDtoResponse>> listarTodos() {
        return ResponseEntity.ok(centroSaludServicio.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener centro de salud por ID")
    public ResponseEntity<CentroSaludDtoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(centroSaludServicio.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar nuevo centro de salud", description = "Exclusivo para administradores del sistema.")
    public ResponseEntity<CentroSaludDtoResponse> crearCentro(@Valid @RequestBody CentroSaludDtoRequest request) {
        return new ResponseEntity<>(centroSaludServicio.crearCentro(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar datos o coordenadas de un centro de salud", description = "Exclusivo para administradores.")
    public ResponseEntity<CentroSaludDtoResponse> actualizarCentro(
            @PathVariable Long id,
            @Valid @RequestBody CentroSaludDtoRequest request) {
        return ResponseEntity.ok(centroSaludServicio.actualizarCentro(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar centro de salud", description = "Exclusivo para administradores.")
    public ResponseEntity<Void> eliminarCentro(@PathVariable Long id) {
        centroSaludServicio.eliminarCentro(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/consultorios")
    @Operation(summary = "Listar consultorios físicos", description = "Permite filtrar opcionalmente por ID de centro de salud.")
    public ResponseEntity<List<ConsultorioFisicoDtoResponse>> listarConsultorios(
            @RequestParam(required = false) Long centroSaludId) {
        return ResponseEntity.ok(centroSaludServicio.listarConsultorios(centroSaludId));
    }

    @PostMapping("/consultorios")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Registrar nuevo consultorio físico en una sede")
    public ResponseEntity<ConsultorioFisicoDtoResponse> crearConsultorio(
            @Valid @RequestBody ConsultorioFisicoDtoRequest request) {
        return new ResponseEntity<>(centroSaludServicio.crearConsultorio(request), HttpStatus.CREATED);
    }

    @DeleteMapping("/consultorios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar consultorio físico")
    public ResponseEntity<Void> eliminarConsultorio(@PathVariable Long id) {
        centroSaludServicio.eliminarConsultorio(id);
        return ResponseEntity.noContent().build();
    }
}
