package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.entidad.Paciente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.SaludUnificada.Esu.seguridad.MascaraDatosUtil;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteTurnosServicio {

    private final TurnoRepositorio turnoRepositorio;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PacienteRepositorio pacienteRepositorio;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private java.time.Clock clock;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private java.time.ZoneId turnosZoneId;

    private java.time.LocalDateTime ahora() {
        java.time.ZoneId zone = turnosZoneId != null ? turnosZoneId : java.time.ZoneId.of("America/Argentina/Cordoba");
        java.time.Clock c = clock != null ? clock.withZone(zone) : java.time.Clock.system(zone);
        return java.time.LocalDateTime.now(c);
    }
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_REGISTROS_REPORTE = 10000;

    private void validarRangoFechas(LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null && hasta != null) {
            if (desde.isAfter(hasta)) {
                throw new ReglaNegocioException("La fecha 'desde' no puede ser posterior a 'hasta'.");
            }
            if (java.time.Duration.between(desde, hasta).toDays() > 180) {
                throw new ReglaNegocioException("El período de consulta de turnos no puede superar los 180 días.");
            }
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportarTurnosExcel(LocalDateTime desde, LocalDateTime hasta) {
        validarRangoFechas(desde, hasta);
        LocalDateTime fechaInicio = desde != null ? desde : ahora().minusMonths(1);
        LocalDateTime fechaFin = hasta != null ? hasta : ahora().plusMonths(3);

        List<Turno> turnos = turnoRepositorio.findReporteTurnosEntre(fechaInicio, fechaFin);
        if (turnos.size() > MAX_REGISTROS_REPORTE) {
            turnos = turnos.subList(0, MAX_REGISTROS_REPORTE);
            log.warn("Se truncó el reporte Excel a {} registros por límite de seguridad operacional.", MAX_REGISTROS_REPORTE);
        }
        log.info("Generando reporte Excel de turnos. Período: {} a {}. Total turnos procesados: {}", fechaInicio, fechaFin, turnos.size());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Turnos");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] columnas = {
                    "ID Turno", "Fecha y Hora", "Estado", "DNI Paciente", "Nombre Paciente",
                    "Email Paciente", "Profesional Médico", "Sobreturno", "Fecha Check-In",
                    "Recepcionista", "Monto Copago", "Nro Bono", "Método Pago", "Observaciones"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Turno t : turnos) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(t.getId() != null ? t.getId() : 0);
                row.createCell(1).setCellValue(t.getFechaHora() != null ? t.getFechaHora().format(DATE_FORMATTER) : "");
                row.createCell(2).setCellValue(t.getEstado() != null ? t.getEstado() : "");

                String dniPaciente = "";
                String nombrePaciente = "";
                String emailPaciente = "";
                if (t.getPaciente() != null) {
                    dniPaciente = t.getPaciente().getDni() != null ? t.getPaciente().getDni() : "";
                    nombrePaciente = (t.getPaciente().getNombre() != null ? t.getPaciente().getNombre() : "") + " " +
                            (t.getPaciente().getApellido() != null ? t.getPaciente().getApellido() : "");
                    if (t.getPaciente().getUsuario() != null) {
                        emailPaciente = t.getPaciente().getUsuario().getEmail() != null ? t.getPaciente().getUsuario().getEmail() : "";
                    }
                }
                row.createCell(3).setCellValue(dniPaciente);
                row.createCell(4).setCellValue(nombrePaciente.trim());
                row.createCell(5).setCellValue(emailPaciente);

                String profNombre = "";
                if (t.getProfesional() != null) {
                    profNombre = (t.getProfesional().getNombre() != null ? t.getProfesional().getNombre() : "") + " " +
                            (t.getProfesional().getApellido() != null ? t.getProfesional().getApellido() : "");
                }
                row.createCell(6).setCellValue(profNombre.trim());
                row.createCell(7).setCellValue(Boolean.TRUE.equals(t.getEsSobreturno()) ? "SÍ" : "NO");
                row.createCell(8).setCellValue(t.getFechaCheckIn() != null ? t.getFechaCheckIn().format(DATE_FORMATTER) : "");
                row.createCell(9).setCellValue(t.getRecepcionistaCheckIn() != null ? t.getRecepcionistaCheckIn() : "");
                row.createCell(10).setCellValue(t.getMontoCopago() != null ? t.getMontoCopago().doubleValue() : 0.0);
                row.createCell(11).setCellValue(t.getNumeroBono() != null ? t.getNumeroBono() : "");
                row.createCell(12).setCellValue(t.getMetodoPago() != null ? t.getMetodoPago() : "");
                row.createCell(13).setCellValue(t.getObservacionesAtencion() != null ? t.getObservacionesAtencion() : "");
            }

            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error al exportar turnos a Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el archivo Excel de turnos", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportarTurnosCsv(LocalDateTime desde, LocalDateTime hasta) {
        validarRangoFechas(desde, hasta);
        LocalDateTime fechaInicio = desde != null ? desde : ahora().minusMonths(1);
        LocalDateTime fechaFin = hasta != null ? hasta : ahora().plusMonths(3);

        List<Turno> turnos = turnoRepositorio.findReporteTurnosEntre(fechaInicio, fechaFin);
        if (turnos.size() > MAX_REGISTROS_REPORTE) {
            turnos = turnos.subList(0, MAX_REGISTROS_REPORTE);
            log.warn("Se truncó el reporte CSV a {} registros por límite de seguridad operacional.", MAX_REGISTROS_REPORTE);
        }
        log.info("Generando reporte CSV de turnos. Período: {} a {}. Total turnos procesados: {}", fechaInicio, fechaFin, turnos.size());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("ID", "Fecha_Hora", "Estado", "DNI_Paciente", "Nombre_Paciente", "Email_Paciente",
                            "Profesional", "Es_Sobreturno", "Fecha_CheckIn", "Recepcionista", "Monto_Copago", "Numero_Bono", "Metodo_Pago")
                    .build();

            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (Turno t : turnos) {
                    String dniPaciente = "";
                    String nombrePaciente = "";
                    String emailPaciente = "";
                    if (t.getPaciente() != null) {
                        dniPaciente = t.getPaciente().getDni() != null ? t.getPaciente().getDni() : "";
                        nombrePaciente = (t.getPaciente().getNombre() != null ? t.getPaciente().getNombre() : "") + " " +
                                (t.getPaciente().getApellido() != null ? t.getPaciente().getApellido() : "");
                        if (t.getPaciente().getUsuario() != null) {
                            emailPaciente = t.getPaciente().getUsuario().getEmail() != null ? t.getPaciente().getUsuario().getEmail() : "";
                        }
                    }

                    String profNombre = "";
                    if (t.getProfesional() != null) {
                        profNombre = (t.getProfesional().getNombre() != null ? t.getProfesional().getNombre() : "") + " " +
                                (t.getProfesional().getApellido() != null ? t.getProfesional().getApellido() : "");
                    }

                    printer.printRecord(
                            t.getId(),
                            t.getFechaHora() != null ? t.getFechaHora().format(DATE_FORMATTER) : "",
                            t.getEstado(),
                            dniPaciente,
                            nombrePaciente.trim(),
                            emailPaciente,
                            profNombre.trim(),
                            Boolean.TRUE.equals(t.getEsSobreturno()) ? "SI" : "NO",
                            t.getFechaCheckIn() != null ? t.getFechaCheckIn().format(DATE_FORMATTER) : "",
                            t.getRecepcionistaCheckIn() != null ? t.getRecepcionistaCheckIn() : "",
                            t.getMontoCopago() != null ? t.getMontoCopago().toString() : "0.00",
                            t.getNumeroBono() != null ? t.getNumeroBono() : "",
                            t.getMetodoPago() != null ? t.getMetodoPago() : ""
                    );
                }
            }
            writer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error al exportar turnos a CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el archivo CSV de turnos", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportarPacientesCsv() {
        List<Paciente> pacientes = pacienteRepositorio.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,DNI,Nombre,Apellido,ObraSocial,NroAfiliado,Telefono,GrupoSanguineo\n");
        for (Paciente p : pacientes) {
            sb.append(p.getId()).append(",")
                    .append(p.getDni()).append(",")
                    .append("\"").append(p.getNombre()).append("\",")
                    .append("\"").append(p.getApellido()).append("\",")
                    .append("\"").append(p.getObraSocial() != null ? p.getObraSocial() : "").append("\",")
                    .append("\"").append(p.getNroAfiliado() != null ? p.getNroAfiliado() : "").append("\",")
                    .append("\"").append(p.getTelefono() != null ? p.getTelefono() : "").append("\",")
                    .append("\"").append(p.getGrupoSanguineo() != null ? p.getGrupoSanguineo() : "").append("\"\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
