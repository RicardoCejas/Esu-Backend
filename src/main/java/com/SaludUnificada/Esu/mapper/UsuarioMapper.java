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
