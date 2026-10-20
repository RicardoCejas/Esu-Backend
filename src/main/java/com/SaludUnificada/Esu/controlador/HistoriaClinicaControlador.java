package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.HistoriaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;
import com.SaludUnificada.Esu.servicio.IHistoriaClinicaServicio;
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
    public ResponseEntity<HistoriaClinicaDtoResponse> crearHistoriaClinica(@RequestBody HistoriaClinicaDtoRequest historiaClinicaDto) {
        HistoriaClinicaDtoResponse nuevaHistoria = historiaClinicaServicio.crearHistoriaClinica(historiaClinicaDto);
        return new ResponseEntity<>(nuevaHistoria, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<HistoriaClinicaDtoResponse> obtenerHistoriaClinicaPorId(@PathVariable Long id) {
        try {
            HistoriaClinicaDtoResponse historia = historiaClinicaServicio.obtenerHistoriaClinicaPorId(id);
            return new ResponseEntity<>(historia, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/obtener/todos")
    public ResponseEntity<List<HistoriaClinicaDtoResponse>> listarTodas() {
        List<HistoriaClinicaDtoResponse> historias = historiaClinicaServicio.listarTodas();
        return new ResponseEntity<>(historias, HttpStatus.OK);
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarHistoriaClinica(@PathVariable Long id) {
        try {
            historiaClinicaServicio.eliminarHistoriaClinica(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<HistoriaClinicaDtoResponse> buscarPorPacienteId(@PathVariable Long pacienteId) {
        try {
            HistoriaClinicaDtoResponse historia = historiaClinicaServicio.buscarPorPacienteId(pacienteId);
            return new ResponseEntity<>(historia, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/rango")
    public ResponseEntity<List<HistoriaClinicaDtoResponse>> buscarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<HistoriaClinicaDtoResponse> historias = historiaClinicaServicio.buscarPorRangoFechas(inicio, fin);
        return new ResponseEntity<>(historias, HttpStatus.OK);
    }
}
