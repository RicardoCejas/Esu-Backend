package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime; // Importación necesaria para LocalDateTime
import java.util.List;
import java.util.Optional;

@Repository
public interface TurnoRepositorio extends JpaRepository<Turno, Long> {
    // Query Method para buscar turnos por el ID del profesional y la fecha y hora
    List<Turno> findByProfesionalIdAndFechaHora(Long profesionalId, LocalDateTime fechaHora); // Corregido

    // Query Method para buscar turnos por el ID del paciente y ordenarlos por fecha y hora descendente
    List<Turno> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);

    boolean existsByProfesionalIdAndFechaHoraAndEstadoNot(Long profesionalId, LocalDateTime fechaHora, String estado);

    List<Turno> findByProfesionalIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
            Long profesionalId, LocalDateTime inicio, LocalDateTime fin, String estado);

    List<Turno> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);
}
