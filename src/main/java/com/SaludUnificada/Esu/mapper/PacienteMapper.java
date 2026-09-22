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
        paciente.setNroAfiliado(dto.getDni()); // Asumiendo que el nro de afiliado es el DNI

        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail() != null && !dto.getEmail().isBlank() ? dto.getEmail() : dto.getDni() + "@esu.com");
        
        String clavePlana = (dto.getPassword() != null && !dto.getPassword().isBlank()) 
                ? dto.getPassword() 
                : dto.getDni(); // Si no se provee clave, se inicializa de forma segura con el DNI encriptado
        usuario.setPassword(passwordEncoder.encode(clavePlana));
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
        if (paciente.getUsuario() != null) {
            dto.setEmail(paciente.getUsuario().getEmail());
        }
        return dto;
    }
}