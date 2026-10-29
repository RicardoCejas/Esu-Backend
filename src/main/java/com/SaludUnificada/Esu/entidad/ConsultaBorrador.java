package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "consulta_borrador", indexes = {
    @Index(name = "idx_borrador_prof", columnList = "profesional_id"),
    @Index(name = "idx_borrador_paciente", columnList = "paciente_id"),
    @Index(name = "idx_borrador_turno", columnList = "turno_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaBorrador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "turno_id")
    private Long turnoId;

    @Column(name = "motivo_consulta")
    private String motivoConsulta;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String anamnesis;

    @Lob
    @Column(name = "examen_fisico", columnDefinition = "TEXT")
    private String examenFisico;

    @Column
    private String diagnostico;

    @Column(name = "codigo_cie10", length = 50)
    private String codigoCie10;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
}
