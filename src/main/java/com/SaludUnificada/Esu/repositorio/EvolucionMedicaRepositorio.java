package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvolucionMedicaRepositorio extends JpaRepository<EvolucionMedica, Long> {
    List<EvolucionMedica> findByPacienteIdOrderByFechaCreacionDesc(Long pacienteId);
    Optional<EvolucionMedica> findByTurnoId(Long turnoId);
    Optional<EvolucionMedica> findByHashIntegridad(String hashIntegridad);
}
