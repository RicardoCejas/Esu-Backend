package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermisoRepositorio extends JpaRepository<Permiso, Long> {
    List<Permiso> findAllByOrderByModuloAscNombreAsc();
    Optional<Permiso> findByCodigo(String codigo);
    Set<Permiso> findByCodigoIn(Set<String> codigos);
}
