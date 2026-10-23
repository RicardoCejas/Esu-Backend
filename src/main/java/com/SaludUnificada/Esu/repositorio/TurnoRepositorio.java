package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TurnoRepositorio extends JpaRepository<Turno, Long> {
    // Query Method para buscar turnos por el ID del profesional y la fecha y hora
    List<Turno> findByProfesionalIdAndFechaHora(Long profesionalId, LocalDateTime fechaHora);

    // Query Method para buscar turnos por el ID del paciente y ordenarlos por fecha y hora descendente
    List<Turno> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);

    // HU-98: Consultas JPA agregadas con COUNT y GROUP BY para métricas y ausentismo
    @Query("SELECT t.estado, COUNT(t) FROM Turno t WHERE t.fechaHora >= :desde AND t.fechaHora <= :hasta GROUP BY t.estado")
    List<Object[]> contarTurnosPorEstadoYFechaHoraEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(t) FROM Turno t WHERE t.fechaHora >= :desde AND t.fechaHora <= :hasta")
    long contarTotalTurnosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
