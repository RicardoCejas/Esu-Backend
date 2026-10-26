package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "plantilla_evolucion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaEvolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "especialidad_id", nullable = true)
    private Especialidad especialidad;

    @Column(name = "estructura_anamnesis", columnDefinition = "TEXT")
    private String estructuraAnamnesis;

    @Column(name = "estructura_examen_fisico", columnDefinition = "TEXT")
    private String estructuraExamenFisico;

    @Column(name = "tratamiento_sugerido", columnDefinition = "TEXT")
    private String tratamientoSugerido;

    @Column(name = "es_publica")
    private Boolean esPublica = true;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (activo == null) {
            activo = true;
        }
        if (esPublica == null) {
            esPublica = true;
        }
    }
}
