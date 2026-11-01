package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepositorio extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.usuario.id = :usuarioId AND r.revocado = false")
    int revocarTodosPorUsuarioId(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.token = :token")
    int revocarPorToken(@Param("token") String token);
}
