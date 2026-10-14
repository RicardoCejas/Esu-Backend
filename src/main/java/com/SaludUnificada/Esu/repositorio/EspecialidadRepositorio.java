package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspecialidadRepositorio extends JpaRepository<Especialidad, Long> {
    List<Especialidad> findByNombreContainingIgnoreCase(String nombre);
}