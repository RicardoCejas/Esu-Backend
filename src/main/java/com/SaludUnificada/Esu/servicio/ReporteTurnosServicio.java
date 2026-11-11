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
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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
        return exportarTurnosExcel(desde, hasta, null);
    }

    @Transactional(readOnly = true)
    public byte[] exportarTurnosExcel(LocalDateTime desde, LocalDateTime hasta, Long centroId) {
        validarRangoFechas(desde, hasta);
        LocalDateTime fechaInicio = desde != null ? desde : ahora().minusMonths(1);
        LocalDateTime fechaFin = hasta != null ? hasta : ahora().plusMonths(3);

        List<Turno> turnos = (centroId != null)
                ? turnoRepositorio.findReporteTurnosEntre(fechaInicio, fechaFin, centroId)
                : turnoRepositorio.findReporteTurnosEntre(fechaInicio, fechaFin);
        if (turnos.size() > MAX_REGISTROS_REPORTE) {
            turnos = turnos.subList(0, MAX_REGISTROS_REPORTE);
            log.warn("Se truncó el reporte Excel a {} registros por límite de seguridad operacional.", MAX_REGISTROS_REPORTE);
        }
        log.info("Generando reporte Excel de turnos. Período: {} a {}. Total turnos procesados: {}", fechaInicio, fechaFin, turnos.size());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean soloAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                && auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RECEPCIONISTA"));

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
                    "ID Turno", "Fecha y Hora", "Centro de Salud", "Tipo Atención", "Servicio", "Estado",
                    "DNI Paciente", "Nombre Paciente", "Email Paciente", "Profesional Médico",
                    "Sobreturno", "Fecha Check-In", "Recepcionista", "Monto Copago", "Nro Bono",
                    "Método Pago", "Observaciones"
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
                row.createCell(2).setCellValue(t.getCentroSaludNombre() != null ? t.getCentroSaludNombre() : (t.getCentroSaludId() != null ? "Centro #" + t.getCentroSaludId() : ""));
                row.createCell(3).setCellValue(t.getTipoAtencion() != null ? t.getTipoAtencion() : "CONSULTA");
                row.createCell(4).setCellValue(t.getServicio() != null ? t.getServicio() : "");
                row.createCell(5).setCellValue(t.getEstado() != null ? t.getEstado() : "");

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
                if (soloAdmin) {
                    dniPaciente = MascaraDatosUtil.enmascararDni(dniPaciente);
                    emailPaciente = MascaraDatosUtil.enmascararEmail(emailPaciente);
                    if (!nombrePaciente.isBlank()) {
                        nombrePaciente = nombrePaciente.trim().charAt(0) + "***";
                    }
                }
                row.createCell(6).setCellValue(dniPaciente);
                row.createCell(7).setCellValue(nombrePaciente.trim());
                row.createCell(8).setCellValue(emailPaciente);

                String profNombre = "";
                if (t.getProfesional() != null) {
                    profNombre = (t.getProfesional().getNombre() != null ? t.getProfesional().getNombre() : "") + " " +
                            (t.getProfesional().getApellido() != null ? t.getProfesional().getApellido() : "");
                }
                row.createCell(9).setCellValue(profNombre.trim());
                row.createCell(10).setCellValue(Boolean.TRUE.equals(t.getEsSobreturno()) ? "SÍ" : "NO");
                row.createCell(11).setCellValue(t.getFechaCheckIn() != null ? t.getFechaCheckIn().format(DATE_FORMATTER) : "");
                row.createCell(12).setCellValue(t.getRecepcionistaCheckIn() != null ? t.getRecepcionistaCheckIn() : "");
                row.createCell(13).setCellValue(t.getMontoCopago() != null ? t.getMontoCopago().doubleValue() : 0.0);
                row.createCell(14).setCellValue(t.getNumeroBono() != null ? t.getNumeroBono() : "");
                row.createCell(15).setCellValue(t.getMetodoPago() != null ? t.getMetodoPago() : "");
                row.createCell(16).setCellValue(t.getObservacionesAtencion() != null ? t.getObservacionesAtencion() : "");
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
        return exportarTurnosCsv(desde, hasta, null);
    }

    @Transactional(readOnly = true)
    public byte[] exportarTurnosCsv(LocalDateTime desde, LocalDateTime hasta, Long centroId) {
        validarRangoFechas(desde, hasta);
        LocalDateTime fechaInicio = desde != null ? desde : ahora().minusMonths(1);
        LocalDateTime fechaFin = hasta != null ? hasta : ahora().plusMonths(3);

        List<Turno> turnos = (centroId != null)
                ? turnoRepositorio.findReporteTurnosEntre(fechaInicio, fechaFin, centroId)
                : turnoRepositorio.findReporteTurnosEntre(fechaInicio, fechaFin);
        if (turnos.size() > MAX_REGISTROS_REPORTE) {
            turnos = turnos.subList(0, MAX_REGISTROS_REPORTE);
            log.warn("Se truncó el reporte CSV a {} registros por límite de seguridad operacional.", MAX_REGISTROS_REPORTE);
        }
        log.info("Generando reporte CSV de turnos. Período: {} a {}. Total turnos procesados: {}", fechaInicio, fechaFin, turnos.size());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean soloAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                && auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RECEPCIONISTA"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("ID", "Fecha_Hora", "Centro_Salud", "Tipo_Atencion", "Servicio", "Estado",
                            "DNI_Paciente", "Nombre_Paciente", "Email_Paciente", "Profesional",
                            "Es_Sobreturno", "Fecha_CheckIn", "Recepcionista", "Monto_Copago", "Numero_Bono", "Metodo_Pago")
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

                    if (soloAdmin) {
                        dniPaciente = MascaraDatosUtil.enmascararDni(dniPaciente);
                        emailPaciente = MascaraDatosUtil.enmascararEmail(emailPaciente);
                        if (!nombrePaciente.isBlank()) {
                            nombrePaciente = nombrePaciente.trim().charAt(0) + "***";
                        }
                    }

                    String profNombre = "";
                    if (t.getProfesional() != null) {
                        profNombre = (t.getProfesional().getNombre() != null ? t.getProfesional().getNombre() : "") + " " +
                                (t.getProfesional().getApellido() != null ? t.getProfesional().getApellido() : "");
                    }

                    String centro = t.getCentroSaludNombre() != null ? t.getCentroSaludNombre() : (t.getCentroSaludId() != null ? "Centro #" + t.getCentroSaludId() : "");
                    String tipoAt = t.getTipoAtencion() != null ? t.getTipoAtencion() : "CONSULTA";
                    String serv = t.getServicio() != null ? t.getServicio() : "";

                    printer.printRecord(
                            t.getId() != null ? t.getId() : "",
                            t.getFechaHora() != null ? t.getFechaHora().format(DATE_FORMATTER) : "",
                            centro,
                            tipoAt,
                            serv,
                            t.getEstado() != null ? t.getEstado() : "",
                            dniPaciente,
                            nombrePaciente.trim(),
                            emailPaciente,
                            profNombre.trim(),
                            Boolean.TRUE.equals(t.getEsSobreturno()) ? "SI" : "NO",
                            t.getFechaCheckIn() != null ? t.getFechaCheckIn().format(DATE_FORMATTER) : "",
                            t.getRecepcionistaCheckIn() != null ? t.getRecepcionistaCheckIn() : "",
                            t.getMontoCopago() != null ? t.getMontoCopago().toString() : "",
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
    public byte[] exportarTurnosPdf(LocalDateTime desde, LocalDateTime hasta) {
        return exportarTurnosPdf(desde, hasta, null);
    }

    @Transactional(readOnly = true)
    public byte[] exportarTurnosPdf(LocalDateTime desde, LocalDateTime hasta, Long centroId) {
        validarRangoFechas(desde, hasta);
        LocalDateTime fechaInicio = desde != null ? desde : ahora().minusMonths(1);
        LocalDateTime fechaFin = hasta != null ? hasta : ahora().plusMonths(3);

        List<Turno> turnos = (centroId != null)
                ? turnoRepositorio.findReporteTurnosEntre(fechaInicio, fechaFin, centroId)
                : turnoRepositorio.findReporteTurnosEntre(fechaInicio, fechaFin);

        if (turnos.size() > MAX_REGISTROS_REPORTE) {
            turnos = turnos.subList(0, MAX_REGISTROS_REPORTE);
            log.warn("Se truncó el reporte PDF a {} registros por límite de seguridad operacional.", MAX_REGISTROS_REPORTE);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean soloAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                && auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RECEPCIONISTA"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new java.awt.Color(2, 132, 199));
            com.lowagie.text.Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, java.awt.Color.DARK_GRAY);
            com.lowagie.text.Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 9, java.awt.Color.GRAY);
            com.lowagie.text.Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, java.awt.Color.WHITE);
            com.lowagie.text.Font tdFont = FontFactory.getFont(FontFactory.HELVETICA, 8, java.awt.Color.BLACK);
            com.lowagie.text.Font tdBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, java.awt.Color.BLACK);

            Paragraph title = new Paragraph("ESU — ECOSISTEMA DE SALUD UNIFICADO", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            String centroTitulo = centroId != null ? "REPORTE OFICIAL DE TURNOS — CENTRO #" + centroId : "REPORTE CONSOLIDADO DE TURNOS — RED SANITARIA";
            Paragraph subTitle = new Paragraph(centroTitulo, subTitleFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subTitle);

            String periodo = String.format("Período: %s a %s | Total turnos: %d | Generado: %s",
                    fechaInicio.format(DATE_FORMATTER), fechaFin.format(DATE_FORMATTER), turnos.size(),
                    ahora().format(DATE_FORMATTER));
            Paragraph info = new Paragraph(periodo, infoFont);
            info.setAlignment(Element.ALIGN_CENTER);
            document.add(info);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.2f, 2.5f, 2.0f, 2.5f, 2.5f, 1.5f, 1.8f});

            String[] headers = {"ID", "Fecha/Hora", "Centro de Salud", "Servicio", "Paciente", "Profesional", "Consultorio", "Estado"};
            java.awt.Color headerColor = new java.awt.Color(2, 132, 199);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, thFont));
                cell.setBackgroundColor(headerColor);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(5f);
                table.addCell(cell);
            }

            java.awt.Color zebra = new java.awt.Color(248, 250, 252);
            int idx = 0;
            for (Turno t : turnos) {
                java.awt.Color bg = (idx++ % 2 == 0) ? java.awt.Color.WHITE : zebra;

                String dniPaciente = (t.getPaciente() != null && t.getPaciente().getDni() != null) ? t.getPaciente().getDni() : "-";
                String nomPaciente = (t.getPaciente() != null)
                        ? ((t.getPaciente().getNombre() != null ? t.getPaciente().getNombre() : "") + " " +
                           (t.getPaciente().getApellido() != null ? t.getPaciente().getApellido() : "")).trim()
                        : "-";
                if (soloAdmin) {
                    dniPaciente = MascaraDatosUtil.enmascararDni(dniPaciente);
                    if (!nomPaciente.isBlank()) nomPaciente = nomPaciente.charAt(0) + "***";
                }

                String nomProf = (t.getProfesional() != null)
                        ? ((t.getProfesional().getNombre() != null ? t.getProfesional().getNombre() : "") + " " +
                           (t.getProfesional().getApellido() != null ? t.getProfesional().getApellido() : "")).trim()
                        : "-";

                String centro = (t.getCentroSaludNombre() != null && !t.getCentroSaludNombre().isBlank())
                        ? t.getCentroSaludNombre()
                        : (t.getCentroSaludId() != null ? "Centro #" + t.getCentroSaludId() : "-");

                String consultorio = (t.getConsultorioNumero() != null && !t.getConsultorioNumero().isBlank())
                        ? "Cons. " + t.getConsultorioNumero()
                        : "-";

                addPdfCell(table, String.valueOf(t.getId() != null ? t.getId() : "-"), tdFont, bg, Element.ALIGN_CENTER);
                addPdfCell(table, t.getFechaHora() != null ? t.getFechaHora().format(DATE_FORMATTER) : "-", tdFont, bg, Element.ALIGN_CENTER);
                addPdfCell(table, centro, tdFont, bg, Element.ALIGN_LEFT);
                addPdfCell(table, t.getServicio() != null ? t.getServicio() : (t.getTipoAtencion() != null ? t.getTipoAtencion() : "CONSULTA"), tdFont, bg, Element.ALIGN_LEFT);
                addPdfCell(table, nomPaciente + " (" + dniPaciente + ")", tdFont, bg, Element.ALIGN_LEFT);
                addPdfCell(table, nomProf, tdFont, bg, Element.ALIGN_LEFT);
                addPdfCell(table, consultorio, tdFont, bg, Element.ALIGN_CENTER);
                addPdfCell(table, t.getEstado() != null ? t.getEstado() : "-", tdBold, bg, Element.ALIGN_CENTER);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF de reporte de turnos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el reporte PDF de turnos", e);
        }
    }

    private void addPdfCell(PdfPTable table, String text, com.lowagie.text.Font font, java.awt.Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    @Transactional(readOnly = true)
    public byte[] exportarPacientesCsv() {
        return exportarPacientesCsv(null);
    }

    @Transactional(readOnly = true)
    public byte[] exportarPacientesCsv(Long centroId) {
        List<Paciente> pacientes;
        if (centroId != null) {
            List<Long> pacienteIds = turnoRepositorio.findDistinctPacienteIdsByCentroSaludId(centroId);
            pacientes = (pacienteIds != null && !pacienteIds.isEmpty() && pacienteRepositorio != null)
                    ? pacienteRepositorio.findAllById(pacienteIds)
                    : java.util.Collections.emptyList();
        } else {
            pacientes = (pacienteRepositorio != null) ? pacienteRepositorio.findAll() : java.util.Collections.emptyList();
        };
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
