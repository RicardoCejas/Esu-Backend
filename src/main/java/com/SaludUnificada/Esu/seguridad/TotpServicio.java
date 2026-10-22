package com.SaludUnificada.Esu.seguridad;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class TotpServicio {

    private static final String ALFABETO_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int PASO_SEGUNDOS = 30;
    private static final int DIGITOS = 6;
    private static final int VENTANA = 1;

    private final SecureRandom random = new SecureRandom();

    public String generarSecreto() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return codificarBase32(bytes);
    }

    public List<String> generarCodigosRespaldo() {
        List<String> codigos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            codigos.add(String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000)));
        }
        return codigos;
    }

    public String generarOtpauthUri(String secreto, String email) {
        return "otpauth://totp/ESU:" + email
                + "?secret=" + secreto
                + "&issuer=ESU&algorithm=SHA1&digits=" + DIGITOS + "&period=" + PASO_SEGUNDOS;
    }

    public String generarQrDataUrl(String otpauthUri) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(otpauthUri, BarcodeFormat.QR_CODE, 240, 240);
            BufferedImage imagen = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, "PNG", salida);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("No se pudo generar el codigo QR para 2FA.", ex);
        }
    }

    public boolean validar(String secreto, String codigo) {
        if (secreto == null || secreto.isBlank() || codigo == null) {
            return false;
        }
        String limpio = codigo.trim();
        if (!limpio.matches("\\d{" + DIGITOS + "}")) {
            return false;
        }

        byte[] clave = decodificarBase32(secreto);
        long contador = Instant.now().getEpochSecond() / PASO_SEGUNDOS;

        for (int desplazamiento = -VENTANA; desplazamiento <= VENTANA; desplazamiento++) {
            String esperado = generarCodigo(clave, contador + desplazamiento);
            if (esperado.equals(limpio)) {
                return true;
            }
        }
        return false;
    }

    private String generarCodigo(byte[] clave, long contador) {
        try {
            byte[] datos = ByteBuffer.allocate(8).putLong(contador).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(clave, "HmacSHA1"));
            byte[] hash = mac.doFinal(datos);

            int offset = hash[hash.length - 1] & 0x0F;
            int binario = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binario % 1_000_000;
            return String.format(Locale.ROOT, "%06d", otp);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular el codigo TOTP.", ex);
        }
    }

    private String codificarBase32(byte[] datos) {
        StringBuilder resultado = new StringBuilder();
        int buffer = 0;
        int bits = 0;

        for (byte dato : datos) {
            buffer = (buffer << 8) | (dato & 0xFF);
            bits += 8;
            while (bits >= 5) {
                int indice = (buffer >> (bits - 5)) & 0x1F;
                bits -= 5;
                resultado.append(ALFABETO_BASE32.charAt(indice));
            }
        }

        if (bits > 0) {
            int indice = (buffer << (5 - bits)) & 0x1F;
            resultado.append(ALFABETO_BASE32.charAt(indice));
        }

        return resultado.toString();
    }

    private byte[] decodificarBase32(String secreto) {
        String limpio = secreto.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;

        for (char caracter : limpio.toCharArray()) {
            int valor = ALFABETO_BASE32.indexOf(caracter);
            if (valor < 0) {
                throw new IllegalArgumentException("Secreto 2FA invalido.");
            }
            buffer = (buffer << 5) | valor;
            bits += 5;
            if (bits >= 8) {
                salida.write((buffer >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }

        return salida.toByteArray();
    }
}
