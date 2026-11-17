package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "vinculo_familiar", indexes = {
    @Index(name = "idx_vinculo_titular", columnList = "titular_id"),
    @Index(name = "idx_vinculo_familiar_dni", columnList = "familiar_dni")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VinculoFamiliar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_id", nullable = false)
    private Paciente titular;

    @Column(name = "familiar_nombre", nullable = false)
    private String familiarNombre;

    @Column(name = "familiar_apellido", nullable = false)
    private String familiarApellido;

    @Column(name = "familiar_dni", nullable = false)
    private String familiarDni;

    @Column(name = "tipo_vinculo", nullable = false)
    private String tipoVinculo; // Hijo/a, Cónyuge, Padre/Madre, Dependiente

    @Column(name = "obra_social")
    private String obraSocial;

    @Column(name = "nro_afiliado")
    private String nroAfiliado;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
