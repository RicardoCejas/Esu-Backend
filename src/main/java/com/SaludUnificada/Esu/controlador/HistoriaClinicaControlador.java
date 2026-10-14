package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.HistoriaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;
import com.SaludUnificada.Esu.servicio.IHistoriaClinicaServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/historias-clinicas")
public class HistoriaClinicaControlador {

    @Autowired
    private IHistoriaClinicaServicio historiaClinicaServicio;

    @PostMapping("/crear")
    public ResponseEntity<HistoriaClinicaDtoResponse> crearHistoriaClinica(@Valid @RequestBody HistoriaClinicaDtoRequest historiaClinicaDto) {
        HistoriaClinicaDtoResponse nuevaHistoria = historiaClinicaServicio.crearHistoriaClinica(historiaClinicaDto);
        return new ResponseEntity<>(nuevaHistoria, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<HistoriaClinicaDtoResponse> obtenerHistoriaClinicaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(historiaClinicaServicio.obtenerHistoriaClinicaPorId(id));
    }

    @GetMapping("/obtener/todos")
    public ResponseEntity<List<HistoriaClinicaDtoResponse>> listarTodas() {
        return ResponseEntity.ok(historiaClinicaServicio.listarTodas());
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarHistoriaClinica(@PathVariable Long id) {
        historiaClinicaServicio.eliminarHistoriaClinica(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<HistoriaClinicaDtoResponse>> buscarPorPacienteId(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(historiaClinicaServicio.buscarPorPacienteId(pacienteId));
    }

    @GetMapping("/rango")
    public ResponseEntity<List<HistoriaClinicaDtoResponse>> buscarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(historiaClinicaServicio.buscarPorRangoFechas(inicio, fin));
    }
}