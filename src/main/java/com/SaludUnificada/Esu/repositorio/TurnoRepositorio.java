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

    List<Turno> findByProfesionalIdAndFechaHora(Long profesionalId, LocalDateTime fechaHora);

    boolean existsByProfesionalIdAndFechaHora(Long profesionalId, LocalDateTime fechaHora);

    List<Turno> findByProfesionalIdAndFechaHoraBetween(Long profesionalId, LocalDateTime inicio, LocalDateTime fin);

    List<Turno> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);

    @Query("SELECT COUNT(t) FROM Turno t WHERE t.profesional.id = :profesionalId " +
           "AND t.esSobreturno = true " +
           "AND t.fechaHora >= :inicioDia AND t.fechaHora <= :finDia " +
           "AND t.estado != 'CANCELADO' AND t.estado != 'CANCELADO_POR_MEDICO'")
    long contarSobreturnosEnDia(
            @Param("profesionalId") Long profesionalId,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);

    @Query("SELECT MIN(t.fechaHora) FROM Turno t WHERE t.profesional.id = :profesionalId " +
           "AND t.estado = 'DISPONIBLE' AND t.fechaHora >= :desde")
    LocalDateTime findPrimerTurnoDisponible(
            @Param("profesionalId") Long profesionalId,
            @Param("desde") LocalDateTime desde);
}
