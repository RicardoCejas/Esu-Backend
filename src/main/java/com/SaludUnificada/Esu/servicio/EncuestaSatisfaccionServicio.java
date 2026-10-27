package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.EncuestaSatisfaccionDtoRequest;
import com.SaludUnificada.Esu.dto.response.EncuestaSatisfaccionDtoResponse;
import com.SaludUnificada.Esu.entidad.EncuestaSatisfaccion;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.EncuestaSatisfaccionRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncuestaSatisfaccionServicio {

    private final EncuestaSatisfaccionRepositorio encuestaRepositorio;
    private final TurnoRepositorio turnoRepositorio;

    @Transactional
    public EncuestaSatisfaccionDtoResponse registrarEncuesta(Long turnoId, EncuestaSatisfaccionDtoRequest request, String emailPaciente) {
        Turno turno = turnoRepositorio.findById(turnoId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + turnoId));

        if (!"ATENDIDO".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("Solo se pueden responder encuestas de satisfacción para turnos que hayan sido atendidos.");
        }

        if (encuestaRepositorio.existsByTurnoId(turnoId)) {
            throw new IllegalStateException("El turno ya cuenta con una encuesta de satisfacción registrada previamente.");
        }

        if (emailPaciente != null && !emailPaciente.isBlank()) {
            if (turno.getPaciente() != null && turno.getPaciente().getUsuario() != null) {
                if (!turno.getPaciente().getUsuario().getEmail().equalsIgnoreCase(emailPaciente)) {
                    throw new org.springframework.security.access.AccessDeniedException("No posee autorización para evaluar la atención de otro paciente.");
                }
            }
        }

        EncuestaSatisfaccion encuesta = EncuestaSatisfaccion.builder()
                .turno(turno)
                .paciente(turno.getPaciente())
                .profesional(turno.getProfesional())
                .puntuacion(request.getPuntuacion())
                .tiempoEsperaPercibido(request.getTiempoEsperaPercibido() != null ? request.getTiempoEsperaPercibido().toUpperCase() : null)
                .comentario(request.getComentario())
                .build();

        EncuestaSatisfaccion guardada = encuestaRepositorio.save(encuesta);
        log.info("Encuesta de satisfacción registrada para turno ID: {}. Puntuación: {}", turnoId, request.getPuntuacion());

        return EncuestaSatisfaccionDtoResponse.builder()
                .id(guardada.getId())
                .turnoId(turno.getId())
                .puntuacion(guardada.getPuntuacion())
                .tiempoEsperaPercibido(guardada.getTiempoEsperaPercibido())
                .comentario(guardada.getComentario())
                .fechaCreacion(guardada.getFechaCreacion())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMetricasSatisfaccion() {
        Double promedioGlobal = encuestaRepositorio.obtenerPromedioPuntuacionGlobal();
        long totalEncuestas = encuestaRepositorio.contarTotalEncuestas();

        Map<String, Object> metricas = new HashMap<>();
        metricas.put("totalEncuestas", totalEncuestas);
        metricas.put("promedioGlobalEstrellas", promedioGlobal != null ? Math.round(promedioGlobal * 100.0) / 100.0 : 0.0);
        metricas.put("npsEstimado", totalEncuestas > 0 ? "SATISFACTORIO" : "SIN_DATOS");
        return metricas;
    }
}
