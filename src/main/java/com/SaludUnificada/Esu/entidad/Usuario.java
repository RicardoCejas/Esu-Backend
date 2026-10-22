package com.SaludUnificada.Esu.entidad;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String rol;

    @Column(name = "estado_activo")
    private Boolean estadoActivo;

    @Column(name = "fecha_alta")
    private LocalDate fechaDeAlta;

    @JsonIgnore
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @JsonIgnore
    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @JsonIgnore
    @Column(name = "two_factor_backup_codes", columnDefinition = "TEXT")
    private String twoFactorBackupCodes;

    @Column(name = "email_verificado")
    private Boolean emailVerificado = false;

    @JsonIgnore
    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @JsonIgnore
    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @JsonIgnore
    @Column(name = "reset_password_expiry")
    private LocalDateTime resetPasswordExpiry;

    // Lado inverso de la relación con Paciente
    @JsonIgnore
    @OneToOne(mappedBy = "usuario")
    private Paciente paciente;

    // Lado inverso de la relación con Profesional
    @JsonIgnore
    @OneToOne(mappedBy = "usuario")
    private Profesional profesional;
}