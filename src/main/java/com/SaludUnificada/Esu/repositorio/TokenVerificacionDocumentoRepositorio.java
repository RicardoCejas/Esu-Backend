package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.TokenVerificacionDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenVerificacionDocumentoRepositorio extends JpaRepository<TokenVerificacionDocumento, Long> {
    Optional<TokenVerificacionDocumento> findByToken(String token);
}
