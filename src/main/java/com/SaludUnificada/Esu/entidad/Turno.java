package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "turno", indexes = {
    @Index(name = "idx_turno_fecha_hora", columnList = "fecha_hora"),
    @Index(name = "idx_turno_estado", columnList = "estado"),
    @Index(name = "idx_turno_paciente_id", columnList = "paciente_id"),
    @Index(name = "idx_turno_profesional_id", columnList = "profesional_id"),
    @Index(name = "idx_turno_codigo_verificacion", columnList = "codigo_verificacion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Turno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private String estado;

    @Column(name = "motivo_turno")
    private String motivoTurno;

    @Column(name = "codigo_verificacion")
    private String codigoVerificacion;

    @Column(name = "obra_social")
    private String obraSocial;

    @Column(name = "numero_afiliado")
    private String numeroAfiliado;

    @Column(name = "copago_monto", precision = 10, scale = 2)
    private BigDecimal copagoMonto;

    @Column(name = "numero_bono")
    private String numeroBono;

    @Column(name = "es_primera_vez")
    private Boolean esPrimeraVez;

    @Column(name = "consultorio_numero")
    private String consultorioNumero;

    @Column(name = "planta")
    private String planta;

    @Column(name = "justificativo_cancelacion", columnDefinition = "TEXT")
    private String justificativoCancelacion;

    @Column(name = "hora_check_in", length = 5)
    private String horaCheckIn;

    @Column(name = "hora_llamado", length = 5)
    private String horaLlamado;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "profesional_id")
    private Profesional profesional;

    @ManyToOne
    @JoinColumn(name = "centro_salud_id")
    private CentroSalud centroSalud;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}