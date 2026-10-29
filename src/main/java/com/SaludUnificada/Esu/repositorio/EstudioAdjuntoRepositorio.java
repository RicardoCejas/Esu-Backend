package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.EstudioAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudioAdjuntoRepositorio extends JpaRepository<EstudioAdjunto, Long> {
    List<EstudioAdjunto> findByPacienteIdOrderByFechaCargaDesc(Long pacienteId);
    List<EstudioAdjunto> findByEvolucionIdOrderByFechaCargaDesc(Long evolucionId);
    long countByPacienteId(Long pacienteId);
}
