package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
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

    // HU-70: Soporte para Autenticación en Dos Pasos (2FA/TOTP)
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    // HU-71: Verificación de Correo Electrónico
    @Column(name = "email_verificado")
    private Boolean emailVerificado = false;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    // HU-71: Recuperación de Contraseña con enlace temporal
    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_expiry")
    private LocalDateTime resetPasswordExpiry;

    // Lado inverso de la relación con Paciente
    @OneToOne(mappedBy = "usuario")
    private Paciente paciente;

    // Lado inverso de la relación con Profesional
    @OneToOne(mappedBy = "usuario")
    private Profesional profesional;
}