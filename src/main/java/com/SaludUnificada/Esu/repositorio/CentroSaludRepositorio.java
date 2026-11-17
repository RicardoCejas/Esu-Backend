package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.CentroSalud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CentroSaludRepositorio extends JpaRepository<CentroSalud, Long> {
    List<CentroSalud> findByActivoTrueOrderByIdAsc();
    boolean existsByNombreIgnoreCase(String nombre);
}
