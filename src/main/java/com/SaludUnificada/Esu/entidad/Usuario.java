package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario", indexes = {
    @Index(name = "idx_usuario_email", columnList = "email"),
    @Index(name = "idx_usuario_rol", columnList = "rol"),
    @Index(name = "idx_usuario_centro", columnList = "centro_salud_id")
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

    @Column(name = "token_version")
    private Integer tokenVersion = 1;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Column(name = "auth_provider")
    private String authProvider = "LOCAL";

    @Column(name = "centro_salud_id")
    private Long centroSaludId;

    @Column(name = "centro_salud_nombre", length = 180)
    private String centroSaludNombre;

    @JsonIgnore
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @JsonIgnore
    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @JsonIgnore
    @Column(name = "two_factor_backup_codes", columnDefinition = "TEXT")
    private String twoFactorBackupCodes;

    @JsonIgnore
    @Column(name = "two_factor_last_counter")
    private Long twoFactorLastCounter;

    public void revocarSesiones() {
        this.tokenVersion = (this.tokenVersion != null ? this.tokenVersion : 0) + 1;
    }

    @OneToOne(mappedBy = "usuario")
    private Paciente paciente;

    @OneToOne(mappedBy = "usuario")
    private Profesional profesional;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_permiso",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "permiso_id")
    )
    private Set<Permiso> permisos = new HashSet<>();
}
