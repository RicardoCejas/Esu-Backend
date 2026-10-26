package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.ConsentimientoInformado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentimientoInformadoRepositorio extends JpaRepository<ConsentimientoInformado, Long> {
    List<ConsentimientoInformado> findByPacienteIdOrderByFechaHoraFirmaDesc(Long pacienteId);
    Optional<ConsentimientoInformado> findByHashIntegridadSha256(String hashIntegridadSha256);
}
