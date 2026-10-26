package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.BloqueoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloqueoAgendaRepositorio extends JpaRepository<BloqueoAgenda, Long> {

    List<BloqueoAgenda> findByProfesionalId(Long profesionalId);

    List<BloqueoAgenda> findByProfesionalIdAndFechaFinAfter(Long profesionalId, LocalDateTime ahora);

    @Query("SELECT b FROM BloqueoAgenda b WHERE b.profesional.id = :profesionalId " +
           "AND :fechaHora >= b.fechaInicio AND :fechaHora < b.fechaFin")
    List<BloqueoAgenda> findBloqueosParaFechaHora(
            @Param("profesionalId") Long profesionalId,
            @Param("fechaHora") LocalDateTime fechaHora);

    @Query("SELECT b FROM BloqueoAgenda b WHERE b.profesional.id = :profesionalId " +
           "AND b.fechaInicio < :hasta AND b.fechaFin > :desde")
    List<BloqueoAgenda> findBloqueosEnRango(
            @Param("profesionalId") Long profesionalId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
