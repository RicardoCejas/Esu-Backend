package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.RecetaDigital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecetaDigitalRepositorio extends JpaRepository<RecetaDigital, Long> {
    List<RecetaDigital> findByPacienteIdOrderByFechaEmisionDesc(Long pacienteId);
    Optional<RecetaDigital> findByHashIntegridad(String hashIntegridad);
}
