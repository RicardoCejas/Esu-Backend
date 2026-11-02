package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "centro_salud", indexes = {
    @Index(name = "idx_centro_salud_tipo", columnList = "tipo"),
    @Index(name = "idx_centro_salud_activo", columnList = "activo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CentroSalud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(length = 80)
    private String telefono;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String ciudad = "Cruz del Eje";

    @Column(name = "horario_atencion", length = 180)
    private String horarioAtencion;

    @Column(name = "disponible_guardia", nullable = false)
    @Builder.Default
    private Boolean disponibleGuardia = false;

    @Column
    private Double latitud;

    @Column
    private Double longitud;

    @Column(name = "permite_consultas", nullable = false)
    @Builder.Default
    private Boolean permiteConsultas = true;

    @Column(name = "permite_estudios", nullable = false)
    @Builder.Default
    private Boolean permiteEstudios = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
