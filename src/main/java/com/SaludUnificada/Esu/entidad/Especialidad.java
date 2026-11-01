package com.SaludUnificada.Esu.entidad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "especialidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Especialidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @ManyToMany(mappedBy = "especialidades")
    @JsonIgnore
    private Set<Profesional> profesionales = new HashSet<>();

    public Especialidad(Long id, String nombre, Set<Profesional> profesionales) {
        this.id = id;
        this.nombre = nombre;
        this.profesionales = profesionales != null ? profesionales : new HashSet<>();
    }
}
