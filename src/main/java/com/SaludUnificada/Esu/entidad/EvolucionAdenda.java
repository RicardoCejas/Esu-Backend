package com.SaludUnificada.Esu.entidad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "evolucion_adenda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionAdenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evolucion_id", nullable = false)
    @JsonIgnore
    private EvolucionMedica evolucionMedica;

    @ManyToOne
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @Column(name = "justificacion_clinica", nullable = false, length = 1000)
    private String justificacionClinica;

    @Column(name = "contenido_adicional", nullable = false, columnDefinition = "TEXT")
    private String contenidoAdicional;

    @Column(name = "fecha_firma", nullable = false)
    private LocalDateTime fechaFirma;

    @Column(name = "hash_integridad", length = 64)
    private String hashIntegridad;

    @PrePersist
    protected void onCreate() {
        if (fechaFirma == null) {
            fechaFirma = LocalDateTime.now();
        }
    }
}
