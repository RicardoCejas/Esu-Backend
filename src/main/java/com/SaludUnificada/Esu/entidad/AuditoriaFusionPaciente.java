package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_fusion_paciente", indexes = {
    @Index(name = "idx_fusion_origen", columnList = "paciente_origen_id"),
    @Index(name = "idx_fusion_destino", columnList = "paciente_destino_id"),
    @Index(name = "idx_fusion_fecha", columnList = "fecha_hora")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaFusionPaciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_origen_id", nullable = false)
    private Long pacienteOrigenId;

    @Column(name = "paciente_destino_id", nullable = false)
    private Long pacienteDestinoId;

    @Column(name = "operador_email", nullable = false, length = 150)
    private String operadorEmail;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(columnDefinition = "TEXT")
    private String detalles;
}
