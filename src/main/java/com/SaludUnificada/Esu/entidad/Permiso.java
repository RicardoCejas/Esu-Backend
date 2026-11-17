package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permiso", indexes = {
    @Index(name = "idx_permiso_modulo", columnList = "modulo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(length = 255)
    private String descripcion;
}
