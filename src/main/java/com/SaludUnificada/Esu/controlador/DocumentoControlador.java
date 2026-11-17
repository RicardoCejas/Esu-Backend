package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.PrevisualizarPdfDtoRequest;
import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.*;
import com.SaludUnificada.Esu.seguridad.RequestIpUtil;
import com.SaludUnificada.Esu.servicio.DocumentoPdfServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@Tag(name = "Documentos y Expedientes Oficiales", description = "Generación, previsualización y streaming de expedientes clínicos foliados (HU-107)")
public class DocumentoControlador {

    private final DocumentoPdfServicio pdfServicio;
    private final PacienteRepositorio pacienteRepositorio;
    private final HistoriaClinicaRepositorio historiaClinicaRepositorio;
    private final EvolucionMedicaRepositorio evolucionMedicaRepositorio;
    private final RecetaDigitalRepositorio recetaDigitalRepositorio;
    private final LogAccesoHcRepositorio logAccesoHcRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final RequestIpUtil requestIpUtil;
    private final TokenVerificacionDocumentoRepositorio tokenVerificacionDocumentoRepositorio;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/historia-clinica/{pacienteId}/pdf-completo")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    @Operation(summary = "Descarga streaming del expediente clínico integral foliado (Página X de Y, QR y Hash SHA-256)")
    public ResponseEntity<StreamingResponseBody> descargarExpedienteCompletoPdf(
            @PathVariable Long pacienteId,
            HttpServletRequest httpRequest) {

        Paciente paciente = pacienteRepositorio.findById(pacienteId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId));

        List<HistoriaClinica> historias = historiaClinicaRepositorio.findByPacienteId(pacienteId);
        HistoriaClinica hc = !historias.isEmpty() ? historias.get(0) : null;

        List<EvolucionMedica> evoluciones = evolucionMedicaRepositorio.findByPacienteIdOrderByFechaCreacionDesc(pacienteId);
        List<RecetaDigital> recetas = recetaDigitalRepositorio.findByPacienteIdOrderByFechaEmisionDesc(pacienteId);

        String randomToken = UUID.randomUUID().toString();
        String tokenHash = calcularSha256(randomToken);
        TokenVerificacionDocumento tokenDoc = new TokenVerificacionDocumento(
                tokenHash,
                paciente.getId(),
                "EXPEDIENTE_CLINICO",
                java.time.LocalDateTime.now().plusDays(7)
        );
        tokenVerificacionDocumentoRepositorio.save(tokenDoc);
        String urlVerificacion = frontendUrl + "/verificar-documento?token=" + randomToken;

        byte[] pdfBytes = pdfServicio.generarExpedienteCompletoPdf(
                paciente, hc, evoluciones, recetas, urlVerificacion
        );

        registrarAuditoriaDescarga(pacienteId, httpRequest);

        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream inputStream = new ByteArrayInputStream(pdfBytes)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        };

        String filename = String.format("expediente_clinico_paciente_%d.pdf", paciente.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentLength(pdfBytes.length)
                .body(responseBody);
    }

    @PostMapping("/previsualizar-pdf")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.canAccessPatient(#request.pacienteId)")
    @Operation(summary = "Previsualizar PDF en vivo antes de la firma digital inalterable")
    public ResponseEntity<byte[]> previsualizarPdf(@Valid @RequestBody PrevisualizarPdfDtoRequest request) {
        if (request == null || request.getPacienteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe especificar un pacienteId válido.");
        }
        Long pacienteId = request.getPacienteId();
        Paciente paciente = pacienteRepositorio.findById(pacienteId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId));

        List<EvolucionMedica> evoluciones = evolucionMedicaRepositorio.findByPacienteIdOrderByFechaCreacionDesc(paciente.getId());
        List<RecetaDigital> recetas = recetaDigitalRepositorio.findByPacienteIdOrderByFechaEmisionDesc(paciente.getId());

        byte[] pdfBytes = pdfServicio.generarExpedienteCompletoPdf(
                paciente, null, evoluciones, recetas, frontendUrl + "/verificar"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "previsualizacion_paciente_" + pacienteId + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    private void registrarAuditoriaDescarga(Long pacienteId, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return;
        }

        Usuario u = usuarioRepositorio.findByEmail(auth.getName()).orElse(null);
        if (u == null) {
            return;
        }

        String ip = requestIpUtil != null ? requestIpUtil.obtenerIpCliente(request) : request.getRemoteAddr();

        LogAccesoHc log = new LogAccesoHc(
                u.getId(),
                pacienteId,
                "DESCARGA_EXPEDIENTE_PDF_COMPLETO",
                ip != null ? ip : "127.0.0.1",
                u.getRol() != null ? u.getRol() : "USUARIO"
        );
        logAccesoHcRepositorio.save(log);
    }

    @GetMapping("/verificar-token/{token}")
    @Operation(summary = "Verificación pública de validez de token de documento oficial")
    public ResponseEntity<Map<String, Object>> verificarTokenPublico(@PathVariable String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("valido", false, "mensaje", "Token inválido"));
        }
        String tokenHash = calcularSha256(token.trim());
        var opt = tokenVerificacionDocumentoRepositorio.findByToken(tokenHash);
        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of("valido", false, "mensaje", "Token de verificación inexistente o inválido."));
        }
        var doc = opt.get();
        doc.setIntentos((doc.getIntentos() != null ? doc.getIntentos() : 0) + 1);
        if (doc.getMaxIntentos() != null && doc.getIntentos() > doc.getMaxIntentos()) {
            doc.setRevocado(true);
            tokenVerificacionDocumentoRepositorio.save(doc);
            return ResponseEntity.ok(Map.of("valido", false, "mensaje", "Límite de intentos de verificación superado. Token revocado por seguridad."));
        }
        if (!doc.esValido()) {
            tokenVerificacionDocumentoRepositorio.save(doc);
            return ResponseEntity.ok(Map.of("valido", false, "mensaje", "Token de verificación expirado o revocado."));
        }
        tokenVerificacionDocumentoRepositorio.save(doc);
        return ResponseEntity.ok(Map.of(
                "valido", true,
                "tipoDocumento", doc.getTipoDocumento(),
                "fechaEmision", doc.getFechaCreacion(),
                "fechaExpiracion", doc.getFechaExpiracion(),
                "mensaje", "Documento oficial íntegro y verificado."
        ));
    }

    private String calcularSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando hash SHA-256", e);
        }
    }
}
