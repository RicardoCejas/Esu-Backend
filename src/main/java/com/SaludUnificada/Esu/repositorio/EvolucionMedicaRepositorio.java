package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.dto.response.TopDiagnosticoDtoResponse;
import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvolucionMedicaRepositorio extends JpaRepository<EvolucionMedica, Long> {
    List<EvolucionMedica> findByPacienteIdOrderByFechaCreacionDesc(Long pacienteId);
    Optional<EvolucionMedica> findByTurnoId(Long turnoId);
    Optional<EvolucionMedica> findByHashIntegridad(String hashIntegridad);

    // HU-98: Consultas JPA agregadas con COUNT y GROUP BY para top patologías CIE-10
    @Query("SELECT new com.SaludUnificada.Esu.dto.response.TopDiagnosticoDtoResponse(e.codigoCie10, e.descripcionCie10, COUNT(e)) " +
           "FROM EvolucionMedica e " +
           "WHERE e.codigoCie10 IS NOT NULL AND TRIM(e.codigoCie10) <> '' " +
           "GROUP BY e.codigoCie10, e.descripcionCie10 " +
           "ORDER BY COUNT(e) DESC")
    List<TopDiagnosticoDtoResponse> obtenerTopDiagnosticosCie10(Pageable pageable);

    @Query("SELECT COUNT(e) FROM EvolucionMedica e WHERE e.codigoCie10 IS NOT NULL AND TRIM(e.codigoCie10) <> ''")
    long contarTotalEvolucionesConCie10();
}
