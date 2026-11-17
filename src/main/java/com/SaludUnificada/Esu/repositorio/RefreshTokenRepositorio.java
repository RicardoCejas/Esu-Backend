package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepositorio extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.usuario.id = :usuarioId AND r.revocado = false")
    int revocarTodosPorUsuarioId(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.tokenFamilyId = :tokenFamilyId AND r.revocado = false")
    int revocarPorTokenFamilyId(@Param("tokenFamilyId") String tokenFamilyId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.tokenHash = :tokenHash")
    int revocarPorTokenHash(@Param("tokenHash") String tokenHash);

    List<RefreshToken> findByTokenFamilyId(String tokenFamilyId);
}
