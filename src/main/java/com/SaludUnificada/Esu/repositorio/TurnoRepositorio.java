package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TurnoRepositorio extends JpaRepository<Turno, Long> {
    List<Turno> findByProfesionalIdAndFechaHora(Long profesionalId, LocalDateTime fechaHora);

    boolean existsByProfesionalIdAndFechaHora(Long profesionalId, LocalDateTime fechaHora);

    List<Turno> findByProfesionalIdAndFechaHoraBetween(Long profesionalId, LocalDateTime inicio, LocalDateTime fin);

    List<Turno> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);
}
