package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.EvolucionAdenda;
import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import com.SaludUnificada.Esu.entidad.MedicamentoReceta;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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
}
