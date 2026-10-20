package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Cie10;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Cie10Repositorio extends JpaRepository<Cie10, Long> {

    Optional<Cie10> findByCodigo(String codigo);

    @Query("SELECT c FROM Cie10 c WHERE LOWER(c.codigo) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Cie10> buscarPorCodigoODescripcion(@Param("query") String query, Pageable pageable);
}
