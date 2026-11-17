package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.Profesional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfesionalRepositorio extends JpaRepository<Profesional, Long> {

    boolean existsByMatricula(String matricula);
    Optional<Profesional> findByMatricula(String matricula);
    List<Profesional> findByEspecialidadesId(Long especialidadId);
    List<Profesional> findByEspecialidades_Id(Long especialidadId);
    Optional<Profesional> findByUsuarioEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profesional p where p.id = :id")
    Optional<Profesional> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Profesional p " +
           "LEFT JOIN FETCH p.especialidades esp " +
           "WHERE p.estadoActivo = true " +
           "AND (:especialidadId IS NULL OR esp.id = :especialidadId) " +
           "AND (:centroSaludId IS NULL OR EXISTS (" +
           "    SELECT 1 FROM DisponibilidadProfesional d " +
           "    WHERE d.profesional = p AND d.activo = true AND d.centroSaludId = :centroSaludId" +
           "))")
    List<Profesional> buscarPrestadoresOptimizado(
            @Param("especialidadId") Long especialidadId,
            @Param("centroSaludId") Long centroSaludId);
}
