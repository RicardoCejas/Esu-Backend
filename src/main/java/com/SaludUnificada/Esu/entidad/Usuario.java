package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario", indexes = {
    @Index(name = "idx_usuario_email", columnList = "email"),
    @Index(name = "idx_usuario_rol", columnList = "rol")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String rol;

    @Column(name = "estado_activo")
    private Boolean estadoActivo;

    @Column(name = "fecha_alta")
    private LocalDate fechaDeAlta;

    @Column(name = "fecha_actualizacion_password")
    private LocalDateTime fechaActualizacionPassword;

    @Column(name = "alto_contraste")
    private Boolean altoContraste = false;

    @Column(name = "tamano_fuente")
    private String tamanoFuente = "normal";

    @Column(name = "avisos_email")
    private Boolean avisosEmail = true;

    @Column(name = "email_verificado")
    private Boolean emailVerificado = false;

    @Column(name = "token_verificacion")
    private String tokenVerificacion;

    @Column(name = "fecha_expiracion_verificacion")
    private LocalDateTime fechaExpiracionVerificacion;

    @Column(name = "token_verificacion_usado")
    private Boolean tokenVerificacionUsado = false;

    @OneToOne(mappedBy = "usuario")
    private Paciente paciente;

    @OneToOne(mappedBy = "usuario")
    private Profesional profesional;
}
