package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TurnoRepositorio extends JpaRepository<Turno, Long> {
    List<Turno> findByProfesionalId(Long profesionalId);

    @Query("SELECT t FROM Turno t WHERE t.profesional.id = :profesionalId AND FUNCTION('DATE', t.fechaHora) = :fecha")
    List<Turno> findByProfesionalIdAndFecha(@Param("profesionalId") Long profesionalId, @Param("fecha") LocalDate fecha);

    List<Turno> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);
}