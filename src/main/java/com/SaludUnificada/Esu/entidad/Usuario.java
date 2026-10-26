package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

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
    private java.time.LocalDateTime fechaActualizacionPassword;

    // HU-BE-108: Preferencias de Usuario y Accesibilidad
    @Column(name = "alto_contraste")
    private Boolean altoContraste = false;

    @Column(name = "tamano_fuente")
    private String tamanoFuente = "normal";

    @Column(name = "avisos_email")
    private Boolean avisosEmail = true;

    // Lado inverso de la relación con Paciente
    @OneToOne(mappedBy = "usuario")
    private Paciente paciente;

    // Lado inverso de la relación con Profesional
    @OneToOne(mappedBy = "usuario")
    private Profesional profesional;
}