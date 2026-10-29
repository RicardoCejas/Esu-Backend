package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;

@Service
public class CarnetDigitalServicio {

    @Autowired
    private PacienteRepositorio pacienteRepositorio;

    @Value("${app.jwt.secret:MiClaveSecretaSuperSeguraParaElEcosistemaDeSaludUnificadoEsu2026}")
    private String hmacKey;

    public byte[] generarQrCarnetDigital(Long pacienteId) {
        Paciente paciente = pacienteRepositorio.findById(pacienteId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId));

        String fechaVigencia = LocalDate.now().plusMonths(6).toString();
        String datos = String.format("ESU|PAC:%d|DNI:%s|AF:%s|OS:%s|VIG:%s",
                paciente.getId(),
                paciente.getDni(),
                paciente.getNroAfiliado(),
                paciente.getObraSocial() != null ? paciente.getObraSocial() : "PARTICULAR",
                fechaVigencia
        );

        String firmaHmac = generarFirmaHmac(datos);
        String payloadCompleto = datos + "|SIG:" + firmaHmac;

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(payloadCompleto, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el codigo QR de la credencial: " + e.getMessage());
        }
    }

    public Map<String, Object> verificarQrCarnet(String qrPayload) {
        if (qrPayload == null || !qrPayload.contains("|SIG:")) {
            return Map.of("valido", false, "motivo", "Formato de credencial no reconocido");
        }

        int sigIndex = qrPayload.lastIndexOf("|SIG:");
        String datos = qrPayload.substring(0, sigIndex);
        String firmaRecibida = qrPayload.substring(sigIndex + 5);

        String firmaEsperada = generarFirmaHmac(datos);
        if (!MessageDigest.isEqual(firmaRecibida.getBytes(StandardCharsets.UTF_8), firmaEsperada.getBytes(StandardCharsets.UTF_8))) {
            return Map.of("valido", false, "motivo", "Firma criptografica invalida o credencial adulterada");
        }

        if (datos.contains("|VIG:")) {
            try {
                int vigIdx = datos.indexOf("|VIG:") + 5;
                String vigStr = datos.substring(vigIdx);
                if (vigStr.contains("|")) {
                    vigStr = vigStr.substring(0, vigStr.indexOf("|"));
                }
                LocalDate vigencia = LocalDate.parse(vigStr);
                if (vigencia.isBefore(LocalDate.now())) {
                    return Map.of("valido", false, "motivo", "Credencial digital vencida el " + vigStr);
                }
            } catch (Exception ignored) {
            }
        }

        return Map.of(
                "valido", true,
                "datos", datos,
                "mensaje", "Credencial de paciente verificada y autentica."
        );
    }

    private String generarFirmaHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(hmacKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error en firma HMAC", e);
        }
    }
}
