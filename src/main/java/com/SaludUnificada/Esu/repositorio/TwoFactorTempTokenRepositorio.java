package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.TwoFactorTempToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TwoFactorTempTokenRepositorio extends JpaRepository<TwoFactorTempToken, Long> {

    Optional<TwoFactorTempToken> findByJti(String jti);

    @Modifying
    @Query("UPDATE TwoFactorTempToken t SET t.consumido = true, t.fechaConsumo = :ahora WHERE t.jti = :jti AND t.consumido = false AND t.fechaExpiracion >= :ahora")
    int consumirTokenAtomicamente(@Param("jti") String jti, @Param("ahora") LocalDateTime ahora);
}
