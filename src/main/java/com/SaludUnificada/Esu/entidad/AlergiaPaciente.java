package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "alergias_pacientes", indexes = {
        @Index(name = "idx_alergia_paciente", columnList = "paciente_id"),
        @Index(name = "idx_alergia_sustancia", columnList = "sustancia_alergeno")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlergiaPaciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "sustancia_alergeno", nullable = false, length = 150)
    private String sustanciaAlergeno;

    @Column(nullable = false, length = 20)
    private String severidad; // LEVE, MODERADA, GRAVE

    @Column(name = "fecha_diagnostico")
    private LocalDate fechaDiagnostico;

    @Column(length = 255)
    private String observaciones;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
        if (this.activo == null) {
            this.activo = true;
        }
        if (this.severidad != null) {
            this.severidad = this.severidad.toUpperCase().trim();
        }
    }
}
