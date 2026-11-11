package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PacienteMapper {

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Paciente paraEntidad(PacienteDtoRequest dto) {
        Paciente paciente = new Paciente();
        paciente.setNombre(dto.getNombre());
        paciente.setApellido(dto.getApellido());
        paciente.setDni(dto.getDni());
        paciente.setTelefono(dto.getTelefono());
        paciente.setNroAfiliado(dto.getNroAfiliado() != null && !dto.getNroAfiliado().isBlank()
                ? dto.getNroAfiliado().trim()
                : dto.getDni());
        paciente.setObraSocial(dto.getObraSocial() != null && !dto.getObraSocial().isBlank()
                ? dto.getObraSocial().trim()
                : "Particular");
        paciente.setGrupoSanguineo(dto.getGrupoSanguineo());
        paciente.setFechaNacimiento(dto.getFechaNacimiento());
        paciente.setCentroSaludReferenciaId(dto.getCentroSaludReferenciaId() != null ? dto.getCentroSaludReferenciaId() : 1L);
        paciente.setCentroSaludReferenciaNombre(dto.getCentroSaludReferenciaNombre() != null && !dto.getCentroSaludReferenciaNombre().isBlank()
                ? dto.getCentroSaludReferenciaNombre().trim()
                : "Hospital Regional Aurelio Crespo");

        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail() != null && !dto.getEmail().isBlank() ? dto.getEmail().trim().toLowerCase() : dto.getDni() + "@esu.com");

        boolean tienePasswordExplicita = dto.getPassword() != null && !dto.getPassword().isBlank();
        if (tienePasswordExplicita) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
            usuario.setEmailVerificado(false);
        } else {
            String claveInusable = java.util.UUID.randomUUID().toString() + "-NO-LOGIN";
            usuario.setPassword(passwordEncoder.encode(claveInusable));
            usuario.setEmailVerificado(false);
        }
        usuario.setRol("PACIENTE");
        usuario.setEstadoActivo(true);
        usuario.setFechaDeAlta(LocalDate.now());

        paciente.setUsuario(usuario);
        return paciente;
    }

    public PacienteDtoResponse paraDto(Paciente paciente) {
        PacienteDtoResponse dto = new PacienteDtoResponse();
        dto.setId(paciente.getId());
        dto.setNombre(paciente.getNombre());
        dto.setApellido(paciente.getApellido());
        dto.setDni(paciente.getDni());
        dto.setTelefono(paciente.getTelefono());
        dto.setObraSocial(paciente.getObraSocial());
        dto.setNroAfiliado(paciente.getNroAfiliado());
        dto.setGrupoSanguineo(paciente.getGrupoSanguineo());
        dto.setFechaNacimiento(paciente.getFechaNacimiento());
        dto.setCentroSaludReferenciaId(paciente.getCentroSaludReferenciaId());
        dto.setCentroSaludReferenciaNombre(paciente.getCentroSaludReferenciaNombre());
        if (paciente.getUsuario() != null) {
            dto.setEmail(paciente.getUsuario().getEmail());
            dto.setActivo(paciente.getUsuario().getEstadoActivo() != null ? paciente.getUsuario().getEstadoActivo() : true);
        } else {
            dto.setActivo(true);
        }
        return dto;
    }
}
