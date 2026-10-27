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
    @Index(name = "idx_turno_prof_fecha_estado", columnList = "profesional_id, fecha_hora, estado"),
    @Index(name = "idx_turno_sobreturno", columnList = "profesional_id, fecha_hora, es_sobreturno")
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

    @Column(name = "es_sobreturno")
    private Boolean esSobreturno = false;

    @Column(name = "motivo_sobreturno")
    private String motivoSobreturno;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "profesional_id")
    private Profesional profesional;

    @Column(name = "fecha_check_in")
    private LocalDateTime fechaCheckIn;

    @Column(name = "recepcionista_check_in")
    private String recepcionistaCheckIn;

    @Column(name = "monto_copago", precision = 10, scale = 2)
    private BigDecimal montoCopago;

    @Column(name = "numero_bono")
    private String numeroBono;

    @Column(name = "metodo_pago")
    private String metodoPago;

    @Column(name = "fecha_copago")
    private LocalDateTime fechaCopago;

    @Column(name = "observaciones_atencion", length = 1000)
    private String observacionesAtencion;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (esSobreturno == null) {
            esSobreturno = false;
        }
    }
}
