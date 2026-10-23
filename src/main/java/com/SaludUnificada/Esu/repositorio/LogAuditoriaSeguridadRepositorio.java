package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.LogAuditoriaSeguridad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogAuditoriaSeguridadRepositorio extends JpaRepository<LogAuditoriaSeguridad, Long> {

    Page<LogAuditoriaSeguridad> findAllByOrderByFechaDesc(Pageable pageable);

    @Query("SELECT COUNT(l) FROM LogAuditoriaSeguridad l WHERE l.email = :email AND l.evento = 'LOGIN_FAIL' AND l.fecha >= :desde")
    long contarIntentosFallidosRecientes(@Param("email") String email, @Param("desde") LocalDateTime desde);

    @Query("SELECT l FROM LogAuditoriaSeguridad l WHERE l.email = :email AND l.fecha >= :desde ORDER BY l.fecha DESC")
    List<LogAuditoriaSeguridad> findEventosRecientesPorEmail(@Param("email") String email, @Param("desde") LocalDateTime desde);
}
