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

    @Query("SELECT t FROM Turno t LEFT JOIN FETCH t.profesional WHERE t.paciente.id = :pacienteId ORDER BY t.fechaHora DESC")
    List<Turno> findByPacienteIdOrderByFechaHoraDesc(@Param("pacienteId") Long pacienteId);

    boolean existsByProfesionalIdAndFechaHoraAndEstadoNot(Long profesionalId, LocalDateTime fechaHora, String estado);

    List<Turno> findByProfesionalIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
            Long profesionalId, LocalDateTime inicio, LocalDateTime fin, String estado);

    List<Turno> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

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

    // HU-98: Consultas JPA agregadas con COUNT y GROUP BY para métricas y ausentismo
    @Query("SELECT t.estado, COUNT(t) FROM Turno t WHERE t.fechaHora >= :desde AND t.fechaHora <= :hasta GROUP BY t.estado")
    List<Object[]> contarTurnosPorEstadoYFechaHoraEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(t) FROM Turno t WHERE t.fechaHora >= :desde AND t.fechaHora <= :hasta")
    long contarTotalTurnosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(t) FROM Turno t WHERE t.paciente.id = :pacienteId AND t.fechaHora >= :desde AND t.estado != 'CANCELADO'")
    long contarTurnosActivosPaciente(@Param("pacienteId") Long pacienteId, @Param("desde") LocalDateTime desde);

    @Query("SELECT t FROM Turno t LEFT JOIN FETCH t.paciente p LEFT JOIN FETCH p.usuario LEFT JOIN FETCH t.profesional prof LEFT JOIN FETCH prof.usuario WHERE t.fechaHora >= :desde AND t.fechaHora <= :hasta ORDER BY t.fechaHora ASC")
    List<Turno> findReporteTurnosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
