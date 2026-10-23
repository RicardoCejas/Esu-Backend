package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRecuperacionRepositorio extends JpaRepository<TokenRecuperacion, Long> {
    Optional<TokenRecuperacion> findByToken(String token);
    Optional<TokenRecuperacion> findByTokenAndUsadoFalse(String token);
}
