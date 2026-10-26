package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.FamiliarDtoRequest;
import com.SaludUnificada.Esu.dto.request.PreferenciasUsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.response.CarnetDigitalDtoResponse;
import com.SaludUnificada.Esu.dto.response.FamiliarDtoResponse;
import com.SaludUnificada.Esu.entidad.ConvenioObraSocial;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.entidad.VinculoFamiliar;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.ConvenioObraSocialRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.repositorio.VinculoFamiliarRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarnetYFamiliarServicio {

    @Autowired
    private PacienteRepositorio pacienteRepositorio;

    @Autowired
    private VinculoFamiliarRepositorio vinculoFamiliarRepositorio;

    @Autowired
    private ConvenioObraSocialRepositorio convenioObraSocialRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    // HU-BE-93: Emisión de Carnet Digital con Token Criptográfico
    @Transactional(readOnly = true)
    public CarnetDigitalDtoResponse emitirCarnetDigital(Long pacienteId) {
        Paciente paciente = pacienteRepositorio.findById(pacienteId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId));

        String rawData = paciente.getDni() + ":" + paciente.getNroAfiliado() + ":" + paciente.getObraSocial();
        String tokenHash = generarHashSha256(rawData);
        String qrPayload = Base64.getEncoder().encodeToString(("ESU:" + paciente.getId() + ":" + tokenHash).getBytes(StandardCharsets.UTF_8));

        return CarnetDigitalDtoResponse.builder()
                .pacienteId(paciente.getId())
                .nombreCompleto(paciente.getNombre() + " " + paciente.getApellido())
                .dni(paciente.getDni())
                .nroAfiliado(paciente.getNroAfiliado())
                .obraSocial(paciente.getObraSocial())
                .grupoSanguineo(paciente.getGrupoSanguineo())
                .fechaEmision(LocalDate.now())
                .estadoActivo(true)
                .tokenVerificacion(tokenHash)
                .qrPayload(qrPayload)
                .build();
    }

    // HU-BE-93: Validación de autenticidad del Carnet
    @Transactional(readOnly = true)
    public boolean verificarCarnet(String qrPayload) {
        try {
            byte[] decoded = Base64.getDecoder().decode(qrPayload);
            String payloadStr = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = payloadStr.split(":");
            if (parts.length < 3 || !parts[0].equals("ESU")) return false;

            Long pacienteId = Long.parseLong(parts[1]);
            String tokenRecibido = parts[2];

            Paciente paciente = pacienteRepositorio.findById(pacienteId).orElse(null);
            if (paciente == null) return false;

            String rawData = paciente.getDni() + ":" + paciente.getNroAfiliado() + ":" + paciente.getObraSocial();
            String tokenEsperado = generarHashSha256(rawData);
            return tokenEsperado.equals(tokenRecibido);
        } catch (Exception e) {
            return false;
        }
    }

    // HU-BE-94: Gestión de Grupo Familiar
    @Transactional
    public FamiliarDtoResponse vincularFamiliar(Long titularId, FamiliarDtoRequest request) {
        Paciente titular = pacienteRepositorio.findById(titularId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente titular no encontrado con ID: " + titularId));

        VinculoFamiliar vinculo = new VinculoFamiliar();
        vinculo.setTitular(titular);
        vinculo.setFamiliarNombre(request.getNombre());
        vinculo.setFamiliarApellido(request.getApellido());
        vinculo.setFamiliarDni(request.getDni());
        vinculo.setTipoVinculo(request.getTipoVinculo());
        vinculo.setObraSocial(request.getObraSocial());
        vinculo.setNroAfiliado(request.getNroAfiliado());

        VinculoFamiliar guardado = vinculoFamiliarRepositorio.save(vinculo);

        return mapFamiliarToDto(guardado);
    }

    @Transactional(readOnly = true)
    public List<FamiliarDtoResponse> listarFamiliares(Long titularId) {
        return vinculoFamiliarRepositorio.findByTitularId(titularId).stream()
                .map(this::mapFamiliarToDto)
                .collect(Collectors.toList());
    }

    // HU-BE-95: Convenios y Coberturas
    @Transactional(readOnly = true)
    public List<ConvenioObraSocial> listarConveniosActivos() {
        return convenioObraSocialRepositorio.findByActivoTrue();
    }

    // HU-BE-108: Preferencias de Usuario y Accesibilidad
    @Transactional
    public void actualizarPreferencias(String email, PreferenciasUsuarioDtoRequest request) {
        Usuario usuario = usuarioRepositorio.findByEmail(email)
                .orElseThrow(() -> new NoEncontradoExcepcion("Usuario no encontrado con email: " + email));

        if (request.getAltoContraste() != null) {
            usuario.setAltoContraste(request.getAltoContraste());
        }
        if (request.getTamanoFuente() != null) {
            usuario.setTamanoFuente(request.getTamanoFuente());
        }
        if (request.getAvisosEmail() != null) {
            usuario.setAvisosEmail(request.getAvisosEmail());
        }

        usuarioRepositorio.save(usuario);
    }

    private FamiliarDtoResponse mapFamiliarToDto(VinculoFamiliar vinculo) {
        return FamiliarDtoResponse.builder()
                .id(vinculo.getId())
                .titularId(vinculo.getTitular().getId())
                .nombre(vinculo.getFamiliarNombre())
                .apellido(vinculo.getFamiliarApellido())
                .dni(vinculo.getFamiliarDni())
                .tipoVinculo(vinculo.getTipoVinculo())
                .obraSocial(vinculo.getObraSocial())
                .nroAfiliado(vinculo.getNroAfiliado())
                .fechaCreacion(vinculo.getFechaCreacion())
                .build();
    }

    private String generarHashSha256(String data) {
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
            throw new RuntimeException("Error calculando hash SHA-256", e);
        }
    }
}
