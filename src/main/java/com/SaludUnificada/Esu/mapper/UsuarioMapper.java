package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.seguridad.Roles;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class UsuarioMapper {

    public Usuario paraEntidad(UsuarioDtoRequest dto) {
        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getUsername());
        usuario.setPassword(dto.getPassword());
        usuario.setRol(dto.getRol() == null || dto.getRol().isBlank()
                ? Roles.PACIENTE
                : Roles.requerido(dto.getRol()));
        usuario.setEstadoActivo(true);
        usuario.setFechaDeAlta(LocalDate.now());
        return usuario;
    }

    public UsuarioDtoResponse paraDto(Usuario usuario) {
        UsuarioDtoResponse dto = new UsuarioDtoResponse();
        dto.setId(usuario.getId());
        dto.setUsername(usuario.getEmail());
        dto.setActivo(usuario.getEstadoActivo());
        return dto;
    }
}
