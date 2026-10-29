package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.NotificacionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionUsuarioRepositorio extends JpaRepository<NotificacionUsuario, Long> {

    List<NotificacionUsuario> findByUsuarioEmailOrderByFechaCreacionDesc(String email);
    List<NotificacionUsuario> findByUsuarioEmailAndLeidaFalseOrderByFechaCreacionDesc(String email);

    long countByUsuarioEmailAndLeidaFalse(String email);

    @Modifying
    @Query("UPDATE NotificacionUsuario n SET n.leida = true WHERE n.usuario.email = :email AND n.leida = false")
    void marcarTodasComoLeidas(@Param("email") String email);
}
