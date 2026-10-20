package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoriaClinicaRepositorio extends JpaRepository<HistoriaClinica, Long> {
    Optional<HistoriaClinica> findByPacienteId(Long pacienteId);
    List<HistoriaClinica> findByFechaAtencionBetween(LocalDateTime inicio, LocalDateTime fin);
}