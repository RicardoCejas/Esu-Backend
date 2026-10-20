package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historia_clinica", indexes = {
    @Index(name = "idx_hc_paciente_id", columnList = "paciente_id"),
    @Index(name = "idx_hc_fecha_atencion", columnList = "fecha_atencion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaClinica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String diagnostico;

    @Column(name = "motivo_consulta")
    private String motivoConsulta;

    @Column
    private String tratamiento;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    @ManyToOne
    @JoinColumn(name = "profesional_id")
    private Profesional profesional;

    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @PrePersist
    protected void onCreate() {
        fechaAtencion = LocalDateTime.now();
    }
}