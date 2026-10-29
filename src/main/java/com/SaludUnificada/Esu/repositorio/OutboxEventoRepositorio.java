package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.OutboxEvento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventoRepositorio extends JpaRepository<OutboxEvento, Long> {
    List<OutboxEvento> findByEstadoOrderByFechaCreacionAsc(String estado, Pageable pageable);
}
