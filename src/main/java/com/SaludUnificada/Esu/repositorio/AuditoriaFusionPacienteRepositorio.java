package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.AuditoriaFusionPaciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaFusionPacienteRepositorio extends JpaRepository<AuditoriaFusionPaciente, Long> {
    List<AuditoriaFusionPaciente> findByPacienteDestinoIdOrderByFechaHoraDesc(Long pacienteDestinoId);
    List<AuditoriaFusionPaciente> findByPacienteOrigenIdOrderByFechaHoraDesc(Long pacienteOrigenId);
}
