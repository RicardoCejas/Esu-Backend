package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.VinculoFamiliar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VinculoFamiliarRepositorio extends JpaRepository<VinculoFamiliar, Long> {
    List<VinculoFamiliar> findByTitularId(Long titularId);
    boolean existsByTitularIdAndFamiliarDni(Long titularId, String familiarDni);
    List<VinculoFamiliar> findByFamiliarDni(String familiarDni);
}
