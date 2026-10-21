package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.ConvenioObraSocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ConvenioObraSocialRepositorio extends JpaRepository<ConvenioObraSocial, Long> {
    Optional<ConvenioObraSocial> findBySiglaIgnoreCase(String sigla);
    List<ConvenioObraSocial> findByActivoTrue();
}
