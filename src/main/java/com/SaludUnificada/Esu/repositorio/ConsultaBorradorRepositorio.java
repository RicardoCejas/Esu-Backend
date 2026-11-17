package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.ConsultaBorrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsultaBorradorRepositorio extends JpaRepository<ConsultaBorrador, Long> {
    Optional<ConsultaBorrador> findByTurnoId(Long turnoId);
    Optional<ConsultaBorrador> findByProfesionalIdAndPacienteId(Long profesionalId, Long pacienteId);
}
