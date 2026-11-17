package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recordatorio_turno", uniqueConstraints = @UniqueConstraint(name = "uk_recordatorio_turno", columnNames = "turno_id"))
@Getter
@Setter
@NoArgsConstructor
public class RecordatorioTurno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @Column(name = "disponible_desde", nullable = false)
    private LocalDateTime disponibleDesde;

    @Column(nullable = false)
    private boolean leido;

    public RecordatorioTurno(Turno turno) {
        this.turno = turno;
        this.disponibleDesde = turno.getFechaHora().minusHours(24);
        this.leido = false;
    }
}
