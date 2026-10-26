package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.RecordatorioTurno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RecordatorioTurnoRepositorio extends JpaRepository<RecordatorioTurno, Long> {
    boolean existsByTurnoId(Long turnoId);
    List<RecordatorioTurno> findByTurnoPacienteIdAndDisponibleDesdeLessThanEqualOrderByDisponibleDesdeAsc(
            Long pacienteId, LocalDateTime ahora);
}
