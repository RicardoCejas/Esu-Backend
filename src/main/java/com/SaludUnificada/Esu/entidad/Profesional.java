package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "profesional", indexes = {
    @Index(name = "idx_profesional_matricula", columnList = "matricula"),
    @Index(name = "idx_profesional_estado_activo", columnList = "estado_activo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Profesional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String matricula;

    @Column
    private String telefono;

    @Column(name = "estado_activo")
    private Boolean estadoActivo;

    // Issue #118: sin cascade REMOVE, la FK en base es ON DELETE SET NULL
    // (borrar un profesional no debe eliminar su usuario).
    @OneToOne
    @JoinColumn(name = "usuario_id", referencedColumnName = "id", unique = true)
    private Usuario usuario;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
        name = "profesional_especialidad",
        joinColumns = { @JoinColumn(name = "profesional_id") },
        inverseJoinColumns = { @JoinColumn(name = "especialidad_id") }
    )
    private Set<Especialidad> especialidades = new HashSet<>();
}