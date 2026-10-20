package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ProfesionalRepositorio extends JpaRepository<Profesional, Long> {
    @Query("SELECT p FROM Profesional p JOIN p.especialidades e WHERE e.id = :especialidadId")
    List<Profesional> findByEspecialidadId(@Param("especialidadId") Long especialidadId);
}