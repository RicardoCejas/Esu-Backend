package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.List; // Importación necesaria para List

@Repository
public interface ProfesionalRepositorio extends JpaRepository<Profesional, Long> {
    // Query Method para buscar profesionales por el ID de una de sus especialidades
    List<Profesional> findByEspecialidades_Id(Long especialidadId); // Corregido

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profesional p where p.id = :id")
    Optional<Profesional> findByIdForUpdate(@Param("id") Long id);
}
