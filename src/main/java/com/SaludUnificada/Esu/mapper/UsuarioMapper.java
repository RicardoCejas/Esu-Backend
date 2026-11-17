package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.entidad.Usuario;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class UsuarioMapper {

    public Usuario paraEntidad(UsuarioDtoRequest dto) {
        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getUsername() != null ? dto.getUsername().trim().toLowerCase() : null);
        usuario.setPassword(dto.getPassword());
        usuario.setNombre(dto.getNombre() != null && !dto.getNombre().isBlank() ? dto.getNombre().trim() : null);
        usuario.setApellido(dto.getApellido() != null && !dto.getApellido().isBlank() ? dto.getApellido().trim() : null);
        String rawDni = dto.getDni() != null ? dto.getDni().trim() : "";
        usuario.setDni(rawDni.isEmpty() ? null : rawDni);
        String rol = (dto.getRol() != null && !dto.getRol().isBlank())
                ? dto.getRol().trim().toUpperCase()
                : "USUARIO";
        usuario.setRol(rol);
        usuario.setEstadoActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setFechaDeAlta(LocalDate.now());
        usuario.setCentroSaludId(dto.getCentroSaludId());
        usuario.setCentroSaludNombre(dto.getCentroSaludNombre());
        return usuario;
    }

    public UsuarioDtoResponse paraDto(Usuario usuario) {
        UsuarioDtoResponse dto = new UsuarioDtoResponse();
        dto.setId(usuario.getId());
        dto.setUsername(usuario.getEmail());
        dto.setEmail(usuario.getEmail());
        String nombre = usuario.getNombre();
        String apellido = usuario.getApellido();
        String dni = usuario.getDni();
        if ((nombre == null || nombre.isBlank()) && usuario.getProfesional() != null) {
            nombre = usuario.getProfesional().getNombre();
            apellido = usuario.getProfesional().getApellido();
        } else if ((nombre == null || nombre.isBlank()) && usuario.getPaciente() != null) {
            nombre = usuario.getPaciente().getNombre();
            apellido = usuario.getPaciente().getApellido();
        }
        if ((dni == null || dni.isBlank()) && usuario.getPaciente() != null) {
            dni = usuario.getPaciente().getDni();
        }
        dto.setNombre(nombre);
        dto.setApellido(apellido);
        dto.setDni(dni);
        dto.setRol(usuario.getRol());
        dto.setActivo(usuario.getEstadoActivo());
        dto.setFechaDeAlta(usuario.getFechaDeAlta());
        dto.setCentroSaludId(usuario.getCentroSaludId());
        dto.setCentroSaludNombre(usuario.getCentroSaludNombre());
        if (usuario.getPermisos() != null) {
            dto.setPermisos(usuario.getPermisos().stream().map(com.SaludUnificada.Esu.entidad.Permiso::getCodigo).sorted().toList());
        }
        return dto;
    }
}
