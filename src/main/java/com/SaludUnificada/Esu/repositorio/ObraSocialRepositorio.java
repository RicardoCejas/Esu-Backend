package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.ObraSocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ObraSocialRepositorio extends JpaRepository<ObraSocial, Long> {
    Optional<ObraSocial> findByCodigo(String codigo);
}
