package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.DisponibilidadProfesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface DisponibilidadProfesionalRepositorio extends JpaRepository<DisponibilidadProfesional, Long> {

    List<DisponibilidadProfesional> findByProfesionalId(Long profesionalId);

    List<DisponibilidadProfesional> findByProfesionalIdAndActivoTrue(Long profesionalId);

    List<DisponibilidadProfesional> findByProfesionalIdAndDiaSemanaAndActivoTrue(Long profesionalId, DayOfWeek diaSemana);

    @Query("SELECT d FROM DisponibilidadProfesional d WHERE d.profesional.id = :profesionalId " +
           "AND d.diaSemana = :diaSemana AND d.activo = true " +
           "AND (d.horaInicio < :horaFin AND d.horaFin > :horaInicio)")
    List<DisponibilidadProfesional> findSolapadas(
            @Param("profesionalId") Long profesionalId,
            @Param("diaSemana") DayOfWeek diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);
}
