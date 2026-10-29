package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.OutboxEvento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventoRepositorio extends JpaRepository<OutboxEvento, Long> {

    List<OutboxEvento> findByEstadoOrderByFechaCreacionAsc(String estado, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OutboxEvento o WHERE o.estado = :estado ORDER BY o.fechaCreacion ASC")
    List<OutboxEvento> findByEstadoForUpdate(@Param("estado") String estado, Pageable pageable);
}
