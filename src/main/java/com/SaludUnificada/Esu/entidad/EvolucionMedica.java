package com.SaludUnificada.Esu.entidad;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evolucion_medica")
@EntityListeners(EvolucionInmutableListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id")
    @JsonIgnoreProperties({"paciente", "profesional"})
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historia_clinica_id")
    @JsonIgnoreProperties({"paciente", "profesional"})
    private HistoriaClinica historiaClinica;

    @Column(name = "motivo_consulta", nullable = false, columnDefinition = "TEXT")
    private String motivoConsulta;

    @Column(columnDefinition = "TEXT")
    private String anamnesis;

    @Column(name = "examen_fisico", columnDefinition = "TEXT")
    private String examenFisico;

    @Column(columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "codigo_cie10", length = 20)
    private String codigoCie10;

    @Column(name = "descripcion_cie10", length = 500)
    private String descripcionCie10;

    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    // Signos Vitales
    @Column(name = "presion_arterial", length = 30)
    private String presionArterial;

    @Column(name = "frecuencia_cardiaca", length = 30)
    private String frecuenciaCardiaca;

    @Column(name = "temperatura", length = 20)
    private String temperatura;

    @Column(name = "peso", length = 20)
    private String peso;

    @Column(name = "talla", length = 20)
    private String talla;

    @Column(name = "imc", length = 20)
    private String imc;

    // Firma e Inmutabilidad
    @Column(nullable = false)
    private Boolean firmada = false;

    @Column(name = "fecha_firma")
    private LocalDateTime fechaFirma;

    @Column(name = "hash_integridad", length = 64)
    private String hashIntegridad;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "evolucionMedica", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EvolucionAdenda> adendas = new ArrayList<>();

    @OneToMany(mappedBy = "evolucionMedica", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("evolucionMedica")
    private List<RecetaDigital> recetas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
