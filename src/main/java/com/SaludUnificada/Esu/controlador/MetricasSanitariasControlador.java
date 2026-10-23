package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.MetricasAusentismoDtoResponse;
import com.SaludUnificada.Esu.dto.response.TopDiagnosticoDtoResponse;
import com.SaludUnificada.Esu.servicio.MetricasSanitariasServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/metricas")
@PreAuthorize("hasRole('ADMIN')")
public class MetricasSanitariasControlador {

    @Autowired
    private MetricasSanitariasServicio metricasServicio;

    /**
     * HU-98: Endpoint de cálculo de ausentismo y distribución de turnos por rango de fechas.
     */
    @GetMapping("/ausentismo")
    public ResponseEntity<MetricasAusentismoDtoResponse> obtenerMetricasAusentismo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long centroId
    ) {
        MetricasAusentismoDtoResponse response = metricasServicio.obtenerMetricasAusentismo(desde, hasta, centroId);
        return ResponseEntity.ok(response);
    }

    /**
     * HU-98: Endpoint de patologías y diagnósticos CIE-10 de mayor recurrencia.
     */
    @GetMapping("/top-diagnosticos")
    public ResponseEntity<List<TopDiagnosticoDtoResponse>> obtenerTopDiagnosticos(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<TopDiagnosticoDtoResponse> top = metricasServicio.obtenerTopDiagnosticos(limit);
        return ResponseEntity.ok(top);
    }
}
