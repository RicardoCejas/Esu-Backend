package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.AlergiaPaciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlergiaPacienteRepositorio extends JpaRepository<AlergiaPaciente, Long> {
    List<AlergiaPaciente> findByPacienteIdAndActivoTrueOrderByFechaRegistroDesc(Long pacienteId);
    List<AlergiaPaciente> findByPacienteIdOrderByFechaRegistroDesc(Long pacienteId);
    boolean existsByPacienteIdAndSustanciaAlergenoIgnoreCaseAndActivoTrue(Long pacienteId, String sustanciaAlergeno);
}
