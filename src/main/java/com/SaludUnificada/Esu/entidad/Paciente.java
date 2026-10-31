package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "paciente", indexes = {
    @Index(name = "idx_paciente_dni", columnList = "dni"),
    @Index(name = "idx_paciente_apellido_nombre", columnList = "apellido, nombre"),
    @Index(name = "idx_paciente_nro_afiliado", columnList = "nro_afiliado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String dni;

    @Column(name = "nro_afiliado", nullable = false, unique = true)
    private String nroAfiliado;

    @Column
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "obra_social")
    private String obraSocial;

    @Column(name = "grupo_sanguineo")
    private String grupoSanguineo;

    @Column(name = "centro_salud_referencia_id")
    private Long centroSaludReferenciaId;

    @Column(name = "centro_salud_referencia_nombre")
    private String centroSaludReferenciaNombre;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fk_usuario", referencedColumnName = "id", nullable = false, unique = true)
    private Usuario usuario;
}