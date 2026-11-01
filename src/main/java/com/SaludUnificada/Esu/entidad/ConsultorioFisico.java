package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "consultorio_fisico", indexes = {
    @Index(name = "idx_consultorio_centro", columnList = "centro_salud_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultorioFisico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "centro_salud_id", nullable = false)
    private Long centroSaludId;

    @Column(nullable = false, length = 50)
    private String numero;

    @Column(nullable = false, length = 80)
    @Builder.Default
    private String planta = "Planta Baja";

    @Column(name = "especialidad_asignada", length = 150)
    private String especialidadAsignada;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
