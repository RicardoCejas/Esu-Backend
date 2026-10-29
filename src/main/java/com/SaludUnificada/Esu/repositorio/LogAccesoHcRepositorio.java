package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.LogAccesoHc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogAccesoHcRepositorio extends JpaRepository<LogAccesoHc, Long> {
    List<LogAccesoHc> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);
    boolean existsByUsuarioIdAndPacienteIdAndAccionAndFechaHoraGreaterThanEqual(
            Long usuarioId, Long pacienteId, String accion, LocalDateTime desde
    );
}
