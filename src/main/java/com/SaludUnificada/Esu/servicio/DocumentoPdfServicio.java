package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.EvolucionAdenda;
import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import com.SaludUnificada.Esu.entidad.HistoriaClinica;
import com.SaludUnificada.Esu.entidad.MedicamentoReceta;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.entidad.Turno;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;

@Service
public class DocumentoPdfServicio {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String calcularHashSha256(String contenido) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(contenido.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al calcular SHA-256", e);
        }
    }

    public byte[] generarQrBytes(String texto, int ancho, int alto) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(texto, BarcodeFormat.QR_CODE, ancho, alto);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar código QR", e);
        }
    }

    public byte[] generarRecetaPdf(RecetaDigital receta, String urlVerificacion) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            // Tipografías
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.NORMAL);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.NORMAL);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.NORMAL);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);

            // Membrete
            Paragraph header = new Paragraph("ESU - ECOSISTEMA DE SALUD UNIFICADO", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph subHeader = new Paragraph("RECETA MÉDICA DIGITAL OFICIAL", subTitleFont);
            subHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(subHeader);

            document.add(new Paragraph(" "));

            // Datos del Profesional y Paciente
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);

            PdfPCell cellMed = new PdfPCell();
            cellMed.setBorder(Rectangle.BOX);
            cellMed.setPadding(8);
            cellMed.addElement(new Paragraph("PROFESIONAL PRESCRIPTOR", headerFont));
            cellMed.addElement(new Paragraph("Dr./Dra: " + receta.getProfesional().getNombre() + " " + receta.getProfesional().getApellido(), textFont));
            cellMed.addElement(new Paragraph("Matrícula: " + receta.getProfesional().getMatricula(), textFont));
            infoTable.addCell(cellMed);

            PdfPCell cellPac = new PdfPCell();
            cellPac.setBorder(Rectangle.BOX);
            cellPac.setPadding(8);
            cellPac.addElement(new Paragraph("DATOS DEL PACIENTE", headerFont));
            cellPac.addElement(new Paragraph("Paciente: " + receta.getPaciente().getNombre() + " " + receta.getPaciente().getApellido(), textFont));
            cellPac.addElement(new Paragraph("DNI: " + receta.getPaciente().getDni() + " | Afiliado: " + receta.getPaciente().getNroAfiliado(), textFont));
            cellPac.addElement(new Paragraph("Obra Social: " + (receta.getPaciente().getObraSocial() != null ? receta.getPaciente().getObraSocial() : "Particular"), textFont));
            infoTable.addCell(cellPac);

            document.add(infoTable);
            document.add(new Paragraph(" "));

            // Fechas y Diagnóstico
            Paragraph fechas = new Paragraph(
                "Fecha de Emisión: " + (receta.getFechaEmision() != null ? receta.getFechaEmision().format(DATE_ONLY_FORMATTER) : "-") +
                "    |    Fecha de Vencimiento: " + (receta.getFechaVencimiento() != null ? receta.getFechaVencimiento().format(DATE_ONLY_FORMATTER) : "-"),
                textFont
            );
            document.add(fechas);

            if (receta.getDiagnostico() != null && !receta.getDiagnostico().isBlank()) {
                Paragraph diag = new Paragraph("Diagnóstico: " + receta.getDiagnostico() +
                    (receta.getCodigoCie10() != null ? " (CIE-10: " + receta.getCodigoCie10() + ")" : ""), textFont);
                document.add(diag);
            }

            document.add(new Paragraph(" "));

            // Tabla de Medicamentos Prescriptos
            PdfPTable medTable = new PdfPTable(5);
            medTable.setWidthPercentage(100);
            medTable.setWidths(new float[]{28, 22, 18, 18, 14});

            String[] headers = {"Droga / Medicamento", "Presentación", "Dosis / Frecuencia", "Vía y Duración", "Cantidad"};
            for (String h : headers) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
                c.setPadding(6);
                medTable.addCell(c);
            }

            if (receta.getMedicamentos() != null) {
                for (MedicamentoReceta m : receta.getMedicamentos()) {
                    medTable.addCell(new Phrase(m.getDroga() + (m.getNombreComercial() != null ? " (" + m.getNombreComercial() + ")" : ""), textFont));
                    medTable.addCell(new Phrase(m.getPresentacion() != null ? m.getPresentacion() : "-", textFont));
                    medTable.addCell(new Phrase((m.getDosisIndicada() != null ? m.getDosisIndicada() : "") + " " + (m.getPosologia() != null ? m.getPosologia() : ""), textFont));
                    medTable.addCell(new Phrase((m.getViaAdministracion() != null ? m.getViaAdministracion() : "") + " " + (m.getDuracion() != null ? m.getDuracion() : ""), textFont));
                    medTable.addCell(new Phrase(m.getCantidad() != null ? m.getCantidad() : "1", textFont));
                }
            }
            document.add(medTable);

            if (receta.getIndicacionesGenerales() != null && !receta.getIndicacionesGenerales().isBlank()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Indicaciones Generales:", headerFont));
                document.add(new Paragraph(receta.getIndicacionesGenerales(), textFont));
            }

            document.add(new Paragraph(" "));

            // Pie con QR y Hash
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{25, 75});

            byte[] qrBytes = generarQrBytes(urlVerificacion, 120, 120);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.scaleToFit(90, 90);

            PdfPCell qrCell = new PdfPCell(qrImage);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerTable.addCell(qrCell);

            PdfPCell hashCell = new PdfPCell();
            hashCell.setBorder(Rectangle.NO_BORDER);
            hashCell.addElement(new Paragraph("FIRMA DIGITAL Y SEGURIDAD LEGAL", headerFont));
            hashCell.addElement(new Paragraph("Documento emitido electrónicamente con validez jurídica conforme a la Ley de Receta Digital.", smallFont));
            hashCell.addElement(new Paragraph("Código Hash SHA-256 de Integridad:", smallFont));
            hashCell.addElement(new Paragraph(receta.getHashIntegridad() != null ? receta.getHashIntegridad() : "-", smallFont));
            hashCell.addElement(new Paragraph("Escanee el código QR o acceda a: " + urlVerificacion, smallFont));
            footerTable.addCell(hashCell);

            document.add(footerTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de receta", e);
        }
    }

    public byte[] generarEvolucionPdf(EvolucionMedica evolucion, String urlVerificacion) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.NORMAL);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.NORMAL);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.NORMAL);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);

            Paragraph header = new Paragraph("ESU - ECOSISTEMA DE SALUD UNIFICADO", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph subHeader = new Paragraph("INFORME DE EVOLUCIÓN CLÍNICA INALTERABLE", subTitleFont);
            subHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(subHeader);

            document.add(new Paragraph(" "));

            // Datos Paciente y Profesional
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);

            PdfPCell c1 = new PdfPCell();
            c1.setPadding(6);
            c1.addElement(new Paragraph("DATOS DEL PACIENTE", headerFont));
            c1.addElement(new Paragraph("Nombre: " + evolucion.getPaciente().getNombre() + " " + evolucion.getPaciente().getApellido(), textFont));
            c1.addElement(new Paragraph("DNI: " + evolucion.getPaciente().getDni(), textFont));
            infoTable.addCell(c1);

            PdfPCell c2 = new PdfPCell();
            c2.setPadding(6);
            c2.addElement(new Paragraph("PROFESIONAL TRATANTE", headerFont));
            c2.addElement(new Paragraph("Dr./Dra: " + evolucion.getProfesional().getNombre() + " " + evolucion.getProfesional().getApellido(), textFont));
            c2.addElement(new Paragraph("Matrícula: " + evolucion.getProfesional().getMatricula(), textFont));
            c2.addElement(new Paragraph("Fecha de Atención: " + (evolucion.getFechaFirma() != null ? evolucion.getFechaFirma().format(DATE_FORMATTER) : "-"), textFont));
            infoTable.addCell(c2);

            document.add(infoTable);
            document.add(new Paragraph(" "));

            // Signos Vitales
            PdfPTable vitals = new PdfPTable(5);
            vitals.setWidthPercentage(100);
            vitals.addCell(new Phrase("TA: " + (evolucion.getPresionArterial() != null ? evolucion.getPresionArterial() : "-"), textFont));
            vitals.addCell(new Phrase("FC: " + (evolucion.getFrecuenciaCardiaca() != null ? evolucion.getFrecuenciaCardiaca() : "-"), textFont));
            vitals.addCell(new Phrase("Temp: " + (evolucion.getTemperatura() != null ? evolucion.getTemperatura() : "-"), textFont));
            vitals.addCell(new Phrase("Peso: " + (evolucion.getPeso() != null ? evolucion.getPeso() : "-"), textFont));
            vitals.addCell(new Phrase("IMC: " + (evolucion.getImc() != null ? evolucion.getImc() : "-"), textFont));
            document.add(vitals);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Motivo de Consulta:", headerFont));
            document.add(new Paragraph(evolucion.getMotivoConsulta() != null ? evolucion.getMotivoConsulta() : "-", textFont));

            if (evolucion.getAnamnesis() != null) {
                document.add(new Paragraph("Anamnesis / Antecedentes:", headerFont));
                document.add(new Paragraph(evolucion.getAnamnesis(), textFont));
            }

            if (evolucion.getExamenFisico() != null) {
                document.add(new Paragraph("Examen Físico:", headerFont));
                document.add(new Paragraph(evolucion.getExamenFisico(), textFont));
            }

            document.add(new Paragraph("Diagnóstico:", headerFont));
            document.add(new Paragraph(
                (evolucion.getDiagnostico() != null ? evolucion.getDiagnostico() : "-") +
                (evolucion.getCodigoCie10() != null ? " [CIE-10: " + evolucion.getCodigoCie10() + " - " + evolucion.getDescripcionCie10() + "]" : ""),
                textFont
            ));

            if (evolucion.getTratamiento() != null) {
                document.add(new Paragraph("Tratamiento / Plan Indicado:", headerFont));
                document.add(new Paragraph(evolucion.getTratamiento(), textFont));
            }

            // Adendas
            if (evolucion.getAdendas() != null && !evolucion.getAdendas().isEmpty()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("ADENDAS CLÍNICAS REGISTRADAS (FE DE ERRATAS):", headerFont));
                for (EvolucionAdenda ad : evolucion.getAdendas()) {
                    document.add(new Paragraph("• Fecha: " + ad.getFechaFirma().format(DATE_FORMATTER) + " | Motivo: " + ad.getJustificacionClinica(), smallFont));
                    document.add(new Paragraph("  Contenido: " + ad.getContenidoAdicional(), textFont));
                }
            }

            document.add(new Paragraph(" "));

            // QR & Footer
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{25, 75});

            byte[] qrBytes = generarQrBytes(urlVerificacion, 120, 120);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.scaleToFit(90, 90);

            PdfPCell qrCell = new PdfPCell(qrImage);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerTable.addCell(qrCell);

            PdfPCell hashCell = new PdfPCell();
            hashCell.setBorder(Rectangle.NO_BORDER);
            hashCell.addElement(new Paragraph("FIRMA ELECTRÓNICA INALTERABLE - LEY 26.529", headerFont));
            hashCell.addElement(new Paragraph("Hash SHA-256 de Integridad Documental:", smallFont));
            hashCell.addElement(new Paragraph(evolucion.getHashIntegridad() != null ? evolucion.getHashIntegridad() : "-", smallFont));
            hashCell.addElement(new Paragraph("Verifique la autenticidad en: " + urlVerificacion, smallFont));
            footerTable.addCell(hashCell);

            document.add(footerTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de evolución clínica", e);
        }
    }

    /**
     * HU-107: Generación de Expediente Clínico Completo Foliado (Página X de Y)
     */
    public byte[] generarExpedienteCompletoPdf(
            Paciente paciente,
            HistoriaClinica hc,
            List<EvolucionMedica> evoluciones,
            List<RecetaDigital> recetas,
            String urlVerificacion) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 45, 45);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Hash provisional para el foliado
            String hashDoc = calcularHashSha256("EXPEDIENTE|" + paciente.getDni() + "|" + System.currentTimeMillis());
            FoliadoPageEventHelper foliadoHelper = new FoliadoPageEventHelper(hashDoc);
            writer.setPageEvent(foliadoHelper);

            document.open();

            // Tipografías
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Font.NORMAL);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.NORMAL);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.NORMAL);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.NORMAL);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);

            // ================= PORTADA INSTITUCIONAL =================
            Paragraph header = new Paragraph("ECOSISTEMA DE SALUD UNIFICADO (ESU) - CRUZ DEL EJE", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph subHeader = new Paragraph("EXPEDIENTE CLÍNICO INTEGRAL OFICIAL FOLIADO", subTitleFont);
            subHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(subHeader);

            Paragraph legalNota = new Paragraph("Validez Legal Plena conforme a Ley Nacional 26.529 de Derechos del Paciente e Historia Clínica", smallFont);
            legalNota.setAlignment(Element.ALIGN_CENTER);
            document.add(legalNota);

            document.add(new Paragraph(" "));

            // Datos del Paciente
            PdfPTable pacTable = new PdfPTable(2);
            pacTable.setWidthPercentage(100);

            PdfPCell c1 = new PdfPCell();
            c1.setPadding(6);
            c1.addElement(new Paragraph("DATOS FILIATORIOS DEL PACIENTE", headerFont));
            c1.addElement(new Paragraph("Nombre Completo: " + paciente.getNombre() + " " + paciente.getApellido(), textFont));
            c1.addElement(new Paragraph("DNI: " + paciente.getDni() + " | Nro Afiliado: " + (paciente.getNroAfiliado() != null ? paciente.getNroAfiliado() : "-"), textFont));
            c1.addElement(new Paragraph("Fecha de Nacimiento: " + (paciente.getFechaNacimiento() != null ? paciente.getFechaNacimiento().format(DATE_ONLY_FORMATTER) : "-"), textFont));
            pacTable.addCell(c1);

            PdfPCell c2 = new PdfPCell();
            c2.setPadding(6);
            c2.addElement(new Paragraph("COBERTURA Y REGISTRO MÉDICO", headerFont));
            c2.addElement(new Paragraph("Obra Social / Prepaga: " + (paciente.getObraSocial() != null ? paciente.getObraSocial() : "Pública / Sin Obra Social"), textFont));
            c2.addElement(new Paragraph("Grupo Sanguíneo: " + (paciente.getGrupoSanguineo() != null ? paciente.getGrupoSanguineo() : "S/D"), textFont));
            c2.addElement(new Paragraph("Teléfono de Contacto: " + (paciente.getTelefono() != null ? paciente.getTelefono() : "-"), textFont));
            pacTable.addCell(c2);

            document.add(pacTable);
            document.add(new Paragraph(" "));

            // Resumen de antecedentes si existe HC
            if (hc != null) {
                PdfPTable hcTable = new PdfPTable(1);
                hcTable.setWidthPercentage(100);
                PdfPCell hcCell = new PdfPCell();
                hcCell.setPadding(6);
                hcCell.addElement(new Paragraph("ANTECEDENTES Y DIAGNÓSTICO BASE DE HISTORIA CLÍNICA:", headerFont));
                hcCell.addElement(new Paragraph("Diagnóstico: " + (hc.getDiagnostico() != null ? hc.getDiagnostico() : "Sin diagnósticos crónicos registrados"), textFont));
                if (hc.getTratamiento() != null) {
                    hcCell.addElement(new Paragraph("Tratamiento de base: " + hc.getTratamiento(), textFont));
                }
                hcTable.addCell(hcCell);
                document.add(hcTable);
                document.add(new Paragraph(" "));
            }

            // ================= EVOLUCIONES CLÍNICAS =================
            Paragraph secEvol = new Paragraph("1. EVOLUCIONES Y REGISTROS DE ATENCIÓN CLÍNICA INALTERABLES", sectionFont);
            document.add(secEvol);
            document.add(new Paragraph(" "));

            if (evoluciones == null || evoluciones.isEmpty()) {
                document.add(new Paragraph("No se registran evoluciones médicas previas cargadas en el sistema para este paciente.", textFont));
            } else {
                for (int i = 0; i < evoluciones.size(); i++) {
                    EvolucionMedica ev = evoluciones.get(i);
                    PdfPTable evTable = new PdfPTable(1);
                    evTable.setWidthPercentage(100);

                    PdfPCell evCell = new PdfPCell();
                    evCell.setPadding(6);

                    String fechaStr = ev.getFechaFirma() != null ? ev.getFechaFirma().format(DATE_FORMATTER) :
                            (ev.getFechaCreacion() != null ? ev.getFechaCreacion().format(DATE_FORMATTER) : "-");
                    String profStr = ev.getProfesional() != null ? "Dr./Dra. " + ev.getProfesional().getNombre() + " " + ev.getProfesional().getApellido() + " (Mat. " + ev.getProfesional().getMatricula() + ")" : "Profesional Asignado";

                    evCell.addElement(new Paragraph("ATENCIÓN #" + (i + 1) + " | Fecha: " + fechaStr + " | Profesional: " + profStr, headerFont));

                    // Signos Vitales
                    String vitals = String.format("TA: %s | FC: %s | Temp: %s | Peso: %s | IMC: %s",
                            ev.getPresionArterial() != null ? ev.getPresionArterial() : "-",
                            ev.getFrecuenciaCardiaca() != null ? ev.getFrecuenciaCardiaca() : "-",
                            ev.getTemperatura() != null ? ev.getTemperatura() : "-",
                            ev.getPeso() != null ? ev.getPeso() : "-",
                            ev.getImc() != null ? ev.getImc() : "-");
                    evCell.addElement(new Paragraph("Signos Vitales: " + vitals, smallFont));

                    evCell.addElement(new Paragraph("Motivo de Consulta: " + (ev.getMotivoConsulta() != null ? ev.getMotivoConsulta() : "-"), textFont));

                    if (ev.getAnamnesis() != null && !ev.getAnamnesis().isBlank()) {
                        evCell.addElement(new Paragraph("Anamnesis: " + ev.getAnamnesis(), textFont));
                    }
                    if (ev.getExamenFisico() != null && !ev.getExamenFisico().isBlank()) {
                        evCell.addElement(new Paragraph("Examen Físico: " + ev.getExamenFisico(), textFont));
                    }

                    String cie = (ev.getCodigoCie10() != null ? " [CIE-10: " + ev.getCodigoCie10() + " - " + (ev.getDescripcionCie10() != null ? ev.getDescripcionCie10() : "") + "]" : "");
                    evCell.addElement(new Paragraph("Diagnóstico: " + (ev.getDiagnostico() != null ? ev.getDiagnostico() : "-") + cie, headerFont));

                    if (ev.getTratamiento() != null && !ev.getTratamiento().isBlank()) {
                        evCell.addElement(new Paragraph("Plan Terapéutico: " + ev.getTratamiento(), textFont));
                    }

                    if (ev.getHashIntegridad() != null) {
                        evCell.addElement(new Paragraph("Firma Electrónica / Hash Integridad: " + ev.getHashIntegridad(), smallFont));
                    }

                    // Adendas
                    if (ev.getAdendas() != null && !ev.getAdendas().isEmpty()) {
                        for (EvolucionAdenda ad : ev.getAdendas()) {
                            evCell.addElement(new Paragraph("  [ADENDA " + ad.getFechaFirma().format(DATE_FORMATTER) + "] Motivo: " + ad.getJustificacionClinica() + " | " + ad.getContenidoAdicional(), smallFont));
                        }
                    }

                    evTable.addCell(evCell);
                    document.add(evTable);
                    document.add(new Paragraph(" "));
                }
            }

            // ================= RECETAS ASOCIADAS =================
            document.add(new Paragraph(" "));
            Paragraph secRec = new Paragraph("2. HISTORIAL DE PRESCRIPCIONES Y RECETAS DIGITALES", sectionFont);
            document.add(secRec);
            document.add(new Paragraph(" "));

            if (recetas == null || recetas.isEmpty()) {
                document.add(new Paragraph("No se registran prescripciones farmacéuticas emitidas para este paciente.", textFont));
            } else {
                PdfPTable recTable = new PdfPTable(5);
                recTable.setWidthPercentage(100);
                recTable.setWidths(new float[]{15, 25, 25, 20, 15});

                recTable.addCell(new PdfPCell(new Paragraph("Emisión", headerFont)));
                recTable.addCell(new PdfPCell(new Paragraph("Profesional", headerFont)));
                recTable.addCell(new PdfPCell(new Paragraph("Medicamentos / Dosis", headerFont)));
                recTable.addCell(new PdfPCell(new Paragraph("Diagnóstico", headerFont)));
                recTable.addCell(new PdfPCell(new Paragraph("Estado", headerFont)));

                for (RecetaDigital r : recetas) {
                    recTable.addCell(new Phrase(r.getFechaEmision() != null ? r.getFechaEmision().format(DATE_ONLY_FORMATTER) : "-", textFont));
                    String profName = r.getProfesional() != null ? r.getProfesional().getNombre() + " " + r.getProfesional().getApellido() : "-";
                    recTable.addCell(new Phrase(profName, textFont));

                    StringBuilder medsText = new StringBuilder();
                    if (r.getMedicamentos() != null) {
                        for (MedicamentoReceta m : r.getMedicamentos()) {
                            medsText.append("• ").append(m.getDroga()).append(" (").append(m.getDosisIndicada() != null ? m.getDosisIndicada() : "").append(")\n");
                        }
                    }
                    recTable.addCell(new Phrase(medsText.toString(), smallFont));
                    recTable.addCell(new Phrase(r.getDiagnostico() != null ? r.getDiagnostico() : "-", smallFont));
                    recTable.addCell(new Phrase(r.getEstadoReceta() != null ? r.getEstadoReceta() : "PENDIENTE", headerFont));
                }
                document.add(recTable);
            }

            // ================= CIERRE DE SEGURIDAD Y QR =================
            document.add(new Paragraph(" "));
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{25, 75});

            byte[] qrBytes = generarQrBytes(urlVerificacion, 120, 120);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.scaleToFit(90, 90);

            PdfPCell qrCell = new PdfPCell(qrImage);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerTable.addCell(qrCell);

            PdfPCell hashCell = new PdfPCell();
            hashCell.setBorder(Rectangle.NO_BORDER);
            hashCell.addElement(new Paragraph("CONSTANCIA DE VALIDEZ Y SEGURIDAD CRIPTOGRÁFICA", headerFont));
            hashCell.addElement(new Paragraph("Hash SHA-256 de Integridad del Expediente Completo:", smallFont));
            hashCell.addElement(new Paragraph(hashDoc, smallFont));
            hashCell.addElement(new Paragraph("Verifique la autenticidad de este expediente en el portal oficial:", smallFont));
            hashCell.addElement(new Paragraph(urlVerificacion, smallFont));
            footerTable.addCell(hashCell);

            document.add(footerTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de expediente clínico foliado", e);
        }
    }

    /**
     * Helper de foliado OpenPDF para numeración 'Página X de Y'
     */

    public byte[] generarCertificadoAsistenciaPdf(Turno turno, String urlVerificacion) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.NORMAL);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.NORMAL);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.NORMAL);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);

            Paragraph header = new Paragraph("ESU - ECOSISTEMA DE SALUD UNIFICADO", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph subHeader = new Paragraph("CERTIFICADO OFICIAL DE ASISTENCIA MÉDICA", subTitleFont);
            subHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(subHeader);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            String nombrePaciente = turno.getPaciente() != null ? turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido() : "PACIENTE";
            String dniPaciente = turno.getPaciente() != null && turno.getPaciente().getDni() != null ? turno.getPaciente().getDni() : "-";
            String nombreMedico = turno.getProfesional() != null ? turno.getProfesional().getNombre() + " " + turno.getProfesional().getApellido() : "PROFESIONAL ACTUANTE";
            String matricula = turno.getProfesional() != null && turno.getProfesional().getMatricula() != null ? turno.getProfesional().getMatricula() : "M.P. PROV";
            String fecha = turno.getFechaHora() != null ? turno.getFechaHora().format(DATE_FORMATTER) : java.time.LocalDate.now().toString();

            String cuerpoTexto = "Por la presente se certifica que el/la paciente " + nombrePaciente + ", DNI Nº " + dniPaciente +
                    ", concurrió a este centro de salud y fue atendido/a en consulta médica el día " + fecha + ".\n\n" +
                    "Se extiende el presente certificado a pedido del/de la interesado/a para ser presentado ante las autoridades laborales, escolares o quien corresponda.";

            Paragraph pCuerpo = new Paragraph(cuerpoTexto, textFont);
            pCuerpo.setLeading(20f);
            pCuerpo.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(pCuerpo);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);

            PdfPCell cellFirma = new PdfPCell();
            cellFirma.setBorder(Rectangle.NO_BORDER);
            cellFirma.addElement(new Paragraph("Firma y Sello Digital:", headerFont));
            cellFirma.addElement(new Paragraph("Dr./Dra. " + nombreMedico, textFont));
            cellFirma.addElement(new Paragraph("Matrícula: " + matricula, textFont));
            cellFirma.addElement(new Paragraph("Estado de Consulta: " + turno.getEstado(), textFont));
            footerTable.addCell(cellFirma);

            PdfPCell cellQr = new PdfPCell();
            cellQr.setBorder(Rectangle.NO_BORDER);
            cellQr.setHorizontalAlignment(Element.ALIGN_RIGHT);
            if (urlVerificacion != null && !urlVerificacion.isBlank()) {
                byte[] qrBytes = generarQrBytes(urlVerificacion, 110, 110);
                if (qrBytes != null) {
                    Image qrImg = Image.getInstance(qrBytes);
                    qrImg.setAlignment(Element.ALIGN_RIGHT);
                    cellQr.addElement(qrImg);
                }
            }
            footerTable.addCell(cellQr);

            document.add(footerTable);

            document.add(new Paragraph(" "));
            Paragraph pAviso = new Paragraph("Documento emitido electrónicamente con validez legal. Código de Validación: TURNO-" + turno.getId() + "-" + Integer.toHexString(turno.hashCode()).toUpperCase(), smallFont);
            pAviso.setAlignment(Element.ALIGN_CENTER);
            document.add(pAviso);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el certificado de asistencia en PDF", e);
        }
    }

    public static class FoliadoPageEventHelper extends PdfPageEventHelper {
        private PdfTemplate totalPagesTemplate;
        private BaseFont baseFont;
        private final String hashResumen;
        private int totalPages = 1;

        public FoliadoPageEventHelper(String hashResumen) {
            this.hashResumen = hashResumen;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                totalPagesTemplate = writer.getDirectContent().createTemplate(30, 16);
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                throw new RuntimeException("Error inicializando foliado OpenPDF", e);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            totalPages = writer.getPageNumber();
            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();

            float textSize = 8;
            float y = document.bottom() - 18;

            // Pie izquierdo
            cb.beginText();
            cb.setFontAndSize(baseFont, 7);
            cb.setTextMatrix(document.left(), y);
            String cortoHash = (hashResumen != null && hashResumen.length() > 16) ? hashResumen.substring(0, 16) + "..." : (hashResumen != null ? hashResumen : "-");
            cb.showText("ESU Cruz del Eje | Expediente Foliado Oficial | Hash Doc: " + cortoHash + " | Ley 26.529");
            cb.endText();

            // Pie derecho: 'Página X de '
            String pageText = "Página " + writer.getPageNumber() + " de ";
            float textWidth = baseFont.getWidthPoint(pageText, textSize);
            float x = document.right() - 55;

            cb.beginText();
            cb.setFontAndSize(baseFont, textSize);
            cb.setTextMatrix(x, y);
            cb.showText(pageText);
            cb.endText();

            cb.addTemplate(totalPagesTemplate, x + textWidth, y);

            cb.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPagesTemplate.beginText();
            totalPagesTemplate.setFontAndSize(baseFont, 8);
            totalPagesTemplate.setTextMatrix(0, 0);
            totalPagesTemplate.showText(String.valueOf(totalPages));
            totalPagesTemplate.endText();
        }
    }
}
