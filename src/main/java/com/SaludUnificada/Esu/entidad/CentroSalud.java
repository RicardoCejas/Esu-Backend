package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "centro_salud", indexes = {
    @Index(name = "idx_centro_salud_nombre", columnList = "nombre"),
    @Index(name = "idx_centro_salud_tipo", columnList = "tipo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CentroSalud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo;

    @Column
    private String direccion;

    @Column
    private String telefono;

    @Column
    private String ciudad;

    @Column(name = "horario_atencion")
    private String horarioAtencion;

    @Column(name = "disponible_guardia")
    private Boolean disponibleGuardia;
}
