package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.ConsentimientoDtoRequest;
import com.SaludUnificada.Esu.dto.response.ConsentimientoDtoResponse;
import com.SaludUnificada.Esu.entidad.ConsentimientoInformado;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.ConsentimientoInformadoRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsentimientoServicio {

    private final ConsentimientoInformadoRepositorio consentimientoRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;

    @Transactional
    public ConsentimientoDtoResponse firmarConsentimiento(ConsentimientoDtoRequest request, String ipFirma) {
        Paciente paciente = pacienteRepositorio.findById(request.getPacienteId())
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + request.getPacienteId()));

        Profesional profesional = profesionalRepositorio.findById(request.getProfesionalId())
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + request.getProfesionalId()));

        LocalDateTime ahora = LocalDateTime.now();
        String ipSegura = (ipFirma != null && !ipFirma.isBlank()) ? ipFirma : "127.0.0.1";

        // Generar sello criptográfico SHA-256 vinculando fecha, IP, partes y contenido legal
        String cadenaSellado = String.format("CONSENTIMIENTO|%d|%d|%s|%s|%s|%s|%s",
                paciente.getId(),
                profesional.getId(),
                request.getProcedimiento().trim(),
                ahora.toString(),
                ipSegura,
                resumenFirma(request.getFirmaDigitalBase64()),
                sha256Hex(request.getTextoLegal())
        );
        String hashSha256 = sha256Hex(cadenaSellado);

        ConsentimientoInformado entidad = new ConsentimientoInformado();
        entidad.setPacienteId(paciente.getId());
        entidad.setProfesionalId(profesional.getId());
        entidad.setProcedimiento(request.getProcedimiento().trim());
        entidad.setTextoLegal(request.getTextoLegal());
        entidad.setFirmaDigitalBase64(request.getFirmaDigitalBase64());
        entidad.setIpFirma(ipSegura);
        entidad.setHashIntegridadSha256(hashSha256);
        entidad.setFechaHoraFirma(ahora);
        entidad.setEstado("FIRMADO");

        ConsentimientoInformado guardado = consentimientoRepositorio.save(entidad);
        return mapearADto(guardado, profesional);
    }

    @Transactional(readOnly = true)
    public List<ConsentimientoDtoResponse> listarPorPaciente(Long pacienteId) {
        if (!pacienteRepositorio.existsById(pacienteId)) {
            throw new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId);
        }
        return consentimientoRepositorio.findByPacienteIdOrderByFechaHoraFirmaDesc(pacienteId)
                .stream()
                .map(c -> {
                    Profesional prof = profesionalRepositorio.findById(c.getProfesionalId()).orElse(null);
                    return mapearADto(c, prof);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConsentimientoDtoResponse obtenerPorId(Long id) {
        ConsentimientoInformado entidad = consentimientoRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Consentimiento informado no encontrado con ID: " + id));
        Profesional prof = profesionalRepositorio.findById(entidad.getProfesionalId()).orElse(null);
        return mapearADto(entidad, prof);
    }

    private ConsentimientoDtoResponse mapearADto(ConsentimientoInformado entidad, Profesional prof) {
        return new ConsentimientoDtoResponse(
                entidad.getId(),
                entidad.getPacienteId(),
                entidad.getProfesionalId(),
                prof != null ? prof.getNombre() + " " + prof.getApellido() : "Profesional #" + entidad.getProfesionalId(),
                prof != null ? prof.getMatricula() : "-",
                entidad.getProcedimiento(),
                entidad.getTextoLegal(),
                entidad.getFirmaDigitalBase64(),
                entidad.getIpFirma(),
                entidad.getHashIntegridadSha256(),
                entidad.getFechaHoraFirma(),
                entidad.getEstado()
        );
    }

    private String resumenFirma(String base64) {
        if (base64 == null) return "";
        return base64.length() > 64 ? base64.substring(0, 64) : base64;
    }

    private String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error inicializando SHA-256", e);
        }
    }
}
