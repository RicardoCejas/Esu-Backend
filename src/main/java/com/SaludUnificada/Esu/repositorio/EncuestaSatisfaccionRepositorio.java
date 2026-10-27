package com.SaludUnificada.Esu.repositorio;

import com.SaludUnificada.Esu.entidad.EncuestaSatisfaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EncuestaSatisfaccionRepositorio extends JpaRepository<EncuestaSatisfaccion, Long> {

    boolean existsByTurnoId(Long turnoId);

    Optional<EncuestaSatisfaccion> findByTurnoId(Long turnoId);

    @Query("SELECT AVG(e.puntuacion) FROM EncuestaSatisfaccion e")
    Double obtenerPromedioPuntuacionGlobal();

    @Query("SELECT AVG(e.puntuacion) FROM EncuestaSatisfaccion e WHERE e.profesional.id = :profesionalId")
    Double obtenerPromedioPuntuacionProfesional(@Param("profesionalId") Long profesionalId);

    @Query("SELECT COUNT(e) FROM EncuestaSatisfaccion e")
    long contarTotalEncuestas();
}
