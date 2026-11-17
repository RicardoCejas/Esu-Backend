package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.PlantillaEvolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlantillaEvolucionRepositorio extends JpaRepository<PlantillaEvolucion, Long> {
    List<PlantillaEvolucion> findByActivoTrue();
    List<PlantillaEvolucion> findByEspecialidadIdAndActivoTrue(Long especialidadId);
    List<PlantillaEvolucion> findByActivoTrueAndTituloContainingIgnoreCase(String titulo);
    boolean existsByTitulo(String titulo);
}
