package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.EvolucionAdenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvolucionAdendaRepositorio extends JpaRepository<EvolucionAdenda, Long> {
    List<EvolucionAdenda> findByEvolucionMedicaIdOrderByFechaFirmaDesc(Long evolucionMedicaId);
}
