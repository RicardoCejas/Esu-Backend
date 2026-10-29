package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.HistoriaClinica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoriaClinicaRepositorio extends JpaRepository<HistoriaClinica, Long> {

    List<HistoriaClinica> findByPacienteId(Long pacienteId);

    List<HistoriaClinica> findByFechaAtencionBetween(LocalDateTime inicio, LocalDateTime fin);
    Page<HistoriaClinica> findByPacienteId(Long pacienteId, Pageable pageable);
    long countByPacienteId(Long pacienteId);
}
