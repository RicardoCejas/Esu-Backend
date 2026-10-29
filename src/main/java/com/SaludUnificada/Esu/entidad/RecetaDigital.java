package com.SaludUnificada.Esu.entidad;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receta_digital")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecetaDigital {

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
    @JoinColumn(name = "evolucion_id")
    @JsonIgnoreProperties({"recetas", "adendas"})
    private EvolucionMedica evolucionMedica;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(length = 500)
    private String diagnostico;

    @Column(name = "codigo_cie10", length = 20)
    private String codigoCie10;

    @Column(name = "indicaciones_generales", columnDefinition = "TEXT")
    private String indicacionesGenerales;

    @Column(name = "estado_receta", length = 20)
    private String estadoReceta = "PENDIENTE";

    @Column(name = "fecha_dispensacion")
    private LocalDateTime fechaDispensacion;

    @Column(name = "matricula_farmacia_dispensadora", length = 50)
    private String matriculaFarmaciaDispensadora;

    @Column(name = "hash_integridad", length = 64)
    private String hashIntegridad;

    @Column(name = "codigo_qr_url", length = 500)
    private String codigoQrUrl;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MedicamentoReceta> medicamentos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (fechaEmision == null) {
            fechaEmision = LocalDate.now();
        }
        if (fechaVencimiento == null) {
            fechaVencimiento = fechaEmision.plusDays(30);
        }
        if (estadoReceta == null) {
            estadoReceta = "PENDIENTE";
        }
    }
}
