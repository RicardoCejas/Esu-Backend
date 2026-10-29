package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.servicio.ReporteTurnosServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes Administrativos", description = "Exportación de turnos y datos asistenciales a Excel y CSV")
public class ReporteControlador {

    private final ReporteTurnosServicio reporteTurnosServicio;

    @Operation(
            summary = "Exportar turnos a Excel o CSV",
            description = "Descarga reporte consolidado de turnos clínicos con datos de pacientes, médicos, copagos y sala de espera."
    )
    @GetMapping("/turnos/exportar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> exportarTurnos(
            @Parameter(description = "Formato de archivo: excel o csv", example = "excel")
            @RequestParam(defaultValue = "excel") String formato,
            @Parameter(description = "Fecha y hora inicial del filtro (opcional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @Parameter(description = "Fecha y hora final del filtro (opcional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {

        String hoy = LocalDate.now().toString();

        if ("csv".equalsIgnoreCase(formato)) {
            byte[] csvData = reporteTurnosServicio.exportarTurnosCsv(desde, hasta);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_turnos_" + hoy + ".csv\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csvData);
        }

        byte[] excelData = reporteTurnosServicio.exportarTurnosExcel(desde, hasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_turnos_" + hoy + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }
    @GetMapping("/pacientes/exportar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> exportarPacientes() {
        byte[] csvData = reporteTurnosServicio.exportarPacientesCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"padron_pacientes.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
