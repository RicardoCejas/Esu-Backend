package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Importación necesaria para List

@Repository
public interface ProfesionalRepositorio extends JpaRepository<Profesional, Long> {
    // Query Method para buscar profesionales por el ID de una de sus especialidades
    List<Profesional> findByEspecialidades_Id(Long especialidadId); // Corregido

    // Query Method para buscar profesionales asignados a un centro de salud
    List<Profesional> findByCentrosSalud_Id(Long centroSaludId);
}