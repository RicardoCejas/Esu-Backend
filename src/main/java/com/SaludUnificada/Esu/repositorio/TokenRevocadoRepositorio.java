package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRevocadoRepositorio extends JpaRepository<TokenRevocado, Long> {

    boolean existsByJti(String jti);

    Optional<TokenRevocado> findByJti(String jti);

    @Modifying
    @Query("DELETE FROM TokenRevocado t WHERE t.fechaExpiracion < :ahora")
    int limpiarTokensExpirados(@Param("ahora") LocalDateTime ahora);
}
