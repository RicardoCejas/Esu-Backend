package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.*;
import com.SaludUnificada.Esu.servicio.DocumentoPdfServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos")
@Tag(name = "Documentos y Expedientes Oficiales", description = "Generación, previsualización y streaming de expedientes clínicos foliados (HU-107)")
public class DocumentoControlador {

    @Autowired
    private DocumentoPdfServicio pdfServicio;

    @Autowired
    private PacienteRepositorio pacienteRepositorio;

    @Autowired
    private HistoriaClinicaRepositorio historiaClinicaRepositorio;

    @Autowired
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    @Autowired
    private RecetaDigitalRepositorio recetaDigitalRepositorio;

    @Autowired
    private LogAccesoHcRepositorio logAccesoHcRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/historia-clinica/{pacienteId}/pdf-completo")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Descarga streaming del expediente clínico integral foliado (Página X de Y, QR y Hash SHA-256)")
    public ResponseEntity<StreamingResponseBody> descargarExpedienteCompletoPdf(
            @PathVariable Long pacienteId,
            HttpServletRequest httpRequest) {

        validarAccesoPaciente(pacienteId);

        Paciente paciente = pacienteRepositorio.findById(pacienteId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId));

        List<HistoriaClinica> historias = historiaClinicaRepositorio.findByPacienteId(pacienteId);
        HistoriaClinica hc = !historias.isEmpty() ? historias.get(0) : null;

        List<EvolucionMedica> evoluciones = evolucionMedicaRepositorio.findByPacienteIdOrderByFechaCreacionDesc(pacienteId);
        List<RecetaDigital> recetas = recetaDigitalRepositorio.findByPacienteIdOrderByFechaEmisionDesc(pacienteId);

        String urlVerificacion = frontendUrl + "/verificar-documento?dni=" + paciente.getDni();

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

        String filename = String.format("expediente_clinico_completo_%s.pdf", paciente.getDni());

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
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Previsualizar PDF en vivo antes de la firma digital inalterable")
    public ResponseEntity<byte[]> previsualizarPdf(@RequestBody Map<String, Object> payload) {
        Long pacienteId = payload.get("pacienteId") != null ? Long.valueOf(payload.get("pacienteId").toString()) : 1L;
        Paciente paciente = pacienteRepositorio.findById(pacienteId).orElse(null);
        if (paciente == null) {
            paciente = new Paciente();
            paciente.setNombre("Paciente");
            paciente.setApellido("Consulta");
            paciente.setDni("00000000");
        }

        List<EvolucionMedica> evoluciones = evolucionMedicaRepositorio.findByPacienteIdOrderByFechaCreacionDesc(paciente.getId());
        List<RecetaDigital> recetas = recetaDigitalRepositorio.findByPacienteIdOrderByFechaEmisionDesc(paciente.getId());

        byte[] pdfBytes = pdfServicio.generarExpedienteCompletoPdf(
                paciente, null, evoluciones, recetas, frontendUrl + "/verificar"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "previsualizacion.pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    private void validarAccesoPaciente(Long pacienteId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Debe autenticarse para acceder al expediente clínico.");
        }

        boolean isStaff = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_MEDICO") ||
                a.getAuthority().equals("ROLE_PROFESIONAL"));

        if (!isStaff) {
            Usuario u = usuarioRepositorio.findByEmail(auth.getName())
                    .orElseThrow(() -> new AccessDeniedException("Usuario no autenticado"));
            if (u.getPaciente() == null || !u.getPaciente().getId().equals(pacienteId)) {
                throw new AccessDeniedException("No posee autorización para consultar el expediente de otro paciente.");
            }
        }
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

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        LogAccesoHc log = new LogAccesoHc(
                u.getId(),
                pacienteId,
                "DESCARGA_EXPEDIENTE_PDF_COMPLETO",
                ip != null ? ip : "127.0.0.1",
                u.getRol() != null ? u.getRol() : "USUARIO"
        );
        logAccesoHcRepositorio.save(log);
    }
}
