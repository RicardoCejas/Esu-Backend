package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.seguridad.OwnershipSecurityEvaluator;
import com.SaludUnificada.Esu.servicio.ReporteTurnosServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping({"/api/admin/reportes", "/api/reportes"})
@RequiredArgsConstructor
@Tag(name = "Reportes Administrativos", description = "Exportación de turnos y datos asistenciales a Excel, CSV y PDF")
public class ReporteControlador {

    private final ReporteTurnosServicio reporteTurnosServicio;

    @Autowired(required = false)
    private OwnershipSecurityEvaluator securityPolicy;

    private Long resolverCentroEfectivo(Long requestedCentroId) {
        if (securityPolicy == null || securityPolicy.isCurrentUserAdmin()) {
            return requestedCentroId;
        }
        return securityPolicy.getAuthenticatedCentroSaludIdOrThrow();
    }

    @Operation(
            summary = "Exportar turnos a Excel, CSV o PDF",
            description = "Descarga reporte consolidado de turnos clínicos con datos de pacientes, médicos, copagos y sala de espera."
    )
    @GetMapping({"/turnos/exportar", "/turnos/resumen"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> exportarTurnos(
            @Parameter(description = "Formato de archivo: excel, csv o pdf", example = "excel")
            @RequestParam(defaultValue = "excel") String formato,
            @Parameter(description = "Fecha y hora inicial del filtro (opcional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @Parameter(description = "Fecha y hora final del filtro (opcional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @Parameter(description = "ID del centro de salud (opcional para ADMIN, forzado al asignado para RECEPCIONISTA)")
            @RequestParam(required = false) Long centroSaludId) {

        String hoy = LocalDate.now().toString();
        Long centroEfectivo = resolverCentroEfectivo(centroSaludId);

        if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdfData = reporteTurnosServicio.exportarTurnosPdf(desde, hasta, centroEfectivo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_turnos_" + hoy + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfData);
        }

        if ("csv".equalsIgnoreCase(formato)) {
            byte[] csvData = reporteTurnosServicio.exportarTurnosCsv(desde, hasta, centroEfectivo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_turnos_" + hoy + ".csv\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csvData);
        }

        byte[] excelData = reporteTurnosServicio.exportarTurnosExcel(desde, hasta, centroEfectivo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_turnos_" + hoy + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/turnos/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> exportarTurnosPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Long centroSaludId) {
        return exportarTurnos("pdf", desde, hasta, centroSaludId);
    }

    @GetMapping("/turnos/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> exportarTurnosCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Long centroSaludId) {
        return exportarTurnos("csv", desde, hasta, centroSaludId);
    }

    @GetMapping("/turnos/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> exportarTurnosExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Long centroSaludId) {
        return exportarTurnos("excel", desde, hasta, centroSaludId);
    }

    @GetMapping({"/pacientes/exportar", "/pacientes/csv"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> exportarPacientes(@RequestParam(required = false) Long centroSaludId) {
        Long centroEfectivo = resolverCentroEfectivo(centroSaludId);
        byte[] csvData = reporteTurnosServicio.exportarPacientesCsv(centroEfectivo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"padron_pacientes.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
