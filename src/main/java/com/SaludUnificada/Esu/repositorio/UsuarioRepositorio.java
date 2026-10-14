package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Importación necesaria para List
import java.util.Optional;

@Repository
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    // Query Method para buscar usuarios por su estado activo
    List<Usuario> findByEstadoActivo(Boolean estadoActivo);
}