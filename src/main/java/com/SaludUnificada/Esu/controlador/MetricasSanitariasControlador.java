package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.MetricasAusentismoDtoResponse;
import com.SaludUnificada.Esu.dto.response.DashboardMetricasDtoResponse;
import com.SaludUnificada.Esu.dto.response.TopDiagnosticoDtoResponse;
import com.SaludUnificada.Esu.seguridad.OwnershipSecurityEvaluator;
import com.SaludUnificada.Esu.servicio.MetricasSanitariasServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metricas")
public class MetricasSanitariasControlador {

    @Autowired
    private MetricasSanitariasServicio metricasServicio;

    @Autowired(required = false)
    private OwnershipSecurityEvaluator securityPolicy;

    private Long resolverCentroEfectivo(Long requestedCentroId) {
        if (securityPolicy == null || securityPolicy.isCurrentUserAdmin()) {
            return requestedCentroId;
        }
        return securityPolicy.getAuthenticatedCentroSaludIdOrThrow();
    }

    @GetMapping("/ausentismo")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<MetricasAusentismoDtoResponse> obtenerMetricasAusentismo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long centroId
    ) {
        MetricasAusentismoDtoResponse response = metricasServicio.obtenerMetricasAusentismo(
                desde, hasta, resolverCentroEfectivo(centroId)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-diagnosticos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TopDiagnosticoDtoResponse>> obtenerTopDiagnosticos(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<TopDiagnosticoDtoResponse> top = metricasServicio.obtenerTopDiagnosticos(limit);
        return ResponseEntity.ok(top);
    }

    @GetMapping("/resumen-ejecutivo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerResumenEjecutivo() {
        return ResponseEntity.ok(metricasServicio.obtenerResumenEjecutivo());
    }

    @GetMapping("/ausentismo/profesional/{profesionalId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('RECEPCIONISTA') and @securityPolicy.isReceptionistOfDoctor(#profesionalId))")
    public ResponseEntity<MetricasAusentismoDtoResponse> obtenerMetricasAusentismoPorProfesional(
            @PathVariable Long profesionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(metricasServicio.obtenerMetricasAusentismoPorProfesional(profesionalId, desde, hasta));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<DashboardMetricasDtoResponse> obtenerDashboardMetricas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long centroId,
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) String consultorio
    ) {
        return ResponseEntity.ok(
                metricasServicio.obtenerDashboardMetricas(
                        desde, hasta, resolverCentroEfectivo(centroId), especialidad, profesionalId, consultorio
                )
        );
    }
}
