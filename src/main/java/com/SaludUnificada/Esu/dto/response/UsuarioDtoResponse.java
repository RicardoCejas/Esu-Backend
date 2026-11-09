package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDtoResponse {
    private Long id;
    private String username;
    private String email;
    private String rol;
    private Boolean activo;
    private LocalDate fechaDeAlta;
    private Long centroSaludId;
    private String centroSaludNombre;
    private java.util.List<String> permisos;

    public UsuarioDtoResponse(Long id, String username, String rol, Boolean activo, LocalDate fechaDeAlta) {
        this.id = id;
        this.username = username;
        this.email = username;
        this.rol = rol;
        this.activo = activo;
        this.fechaDeAlta = fechaDeAlta;
    }

    public UsuarioDtoResponse(Long id, String username, String rol, Boolean activo, LocalDate fechaDeAlta, Long centroSaludId, String centroSaludNombre) {
        this.id = id;
        this.username = username;
        this.email = username;
        this.rol = rol;
        this.activo = activo;
        this.fechaDeAlta = fechaDeAlta;
        this.centroSaludId = centroSaludId;
        this.centroSaludNombre = centroSaludNombre;
    }
}
