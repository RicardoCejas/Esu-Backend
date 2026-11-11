package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByEstadoActivo(Boolean estadoActivo);

    Optional<Usuario> findByTokenVerificacion(String tokenVerificacion);

    Optional<Usuario> findByGoogleSub(String googleSub);

    List<Usuario> findByCentroSaludId(Long centroSaludId);
}
