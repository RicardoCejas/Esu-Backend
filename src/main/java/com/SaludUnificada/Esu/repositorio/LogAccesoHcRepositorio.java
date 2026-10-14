package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.LogAccesoHc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogAccesoHcRepositorio extends JpaRepository<LogAccesoHc, Long> {
    List<LogAccesoHc> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);
}
