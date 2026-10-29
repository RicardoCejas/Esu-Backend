package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.RecetaDigital;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecetaDigitalRepositorio extends JpaRepository<RecetaDigital, Long> {

    List<RecetaDigital> findByPacienteIdOrderByFechaEmisionDesc(Long pacienteId);

    Page<RecetaDigital> findByPacienteId(Long pacienteId, Pageable pageable);

    Optional<RecetaDigital> findByHashIntegridad(String hashIntegridad);

    boolean existsByProfesionalIdAndPacienteId(Long profesionalId, Long pacienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecetaDigital r WHERE r.id = :id")
    Optional<RecetaDigital> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecetaDigital r WHERE r.hashIntegridad = :hashIntegridad")
    Optional<RecetaDigital> findByHashIntegridadForUpdate(@Param("hashIntegridad") String hashIntegridad);

    List<RecetaDigital> findByEstadoRecetaAndFechaVencimientoLessThan(String estadoReceta, LocalDate fecha);

    List<RecetaDigital> findByCodigoCie10(String codigoCie10);
}
