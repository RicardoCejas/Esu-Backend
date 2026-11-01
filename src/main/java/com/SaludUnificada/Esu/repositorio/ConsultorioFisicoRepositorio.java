package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.ConsultorioFisico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultorioFisicoRepositorio extends JpaRepository<ConsultorioFisico, Long> {
    List<ConsultorioFisico> findByCentroSaludIdOrderByNumeroAsc(Long centroSaludId);
    List<ConsultorioFisico> findByActivoTrueOrderByCentroSaludIdAscNumeroAsc();
}
