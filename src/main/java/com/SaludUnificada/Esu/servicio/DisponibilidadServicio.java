package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.DisponibilidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.DisponibilidadDtoResponse;
import com.SaludUnificada.Esu.entidad.DisponibilidadProfesional;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.DisponibilidadMapper;
import com.SaludUnificada.Esu.repositorio.BloqueoAgendaRepositorio;
import com.SaludUnificada.Esu.repositorio.DisponibilidadProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadServicio implements IDisponibilidadServicio {

    private static final Logger log = LoggerFactory.getLogger(DisponibilidadServicio.class);

    private final DisponibilidadProfesionalRepositorio disponibilidadRepositorio;
    private final BloqueoAgendaRepositorio bloqueoAgendaRepositorio;
    private final TurnoRepositorio turnoRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final DisponibilidadMapper disponibilidadMapper;
    private final FeriadoServicio feriadoServicio;

    @Override
    @Transactional
    public DisponibilidadDtoResponse configurarDisponibilidad(DisponibilidadDtoRequest request) {
        if (!request.getHoraInicio().isBefore(request.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio (" + request.getHoraInicio() +
                    ") debe ser estrictamente anterior a la hora de fin (" + request.getHoraFin() + ").");
        }

        Profesional profesional = profesionalRepositorio.findById(request.getProfesionalId())
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + request.getProfesionalId()));

        // Validación de no solapamiento para el mismo profesional y día de atención
        List<DisponibilidadProfesional> solapadas = disponibilidadRepositorio.findSolapadas(
                profesional.getId(),
                request.getDiaSemana(),
                request.getHoraInicio(),
                request.getHoraFin()
        );

        if (!solapadas.isEmpty()) {
            throw new IllegalStateException("Existe un conflicto de horarios: el profesional ya posee una disponibilidad configurada que se solapa con el rango solicitado para el día " + request.getDiaSemana());
        }

        DisponibilidadProfesional entidad = disponibilidadMapper.paraEntidad(request, profesional);
        DisponibilidadProfesional guardada = disponibilidadRepositorio.save(entidad);

        // Generar automáticamente los cupos libres para los próximos 30 días basándose en la grilla
        int slotsGenerados = generarSlotsProximos30Dias(profesional.getId());
        log.info("Disponibilidad registrada para el profesional {}. Se generaron {} slots de turnos para los próximos 30 días.",
                profesional.getId(), slotsGenerados);

        return disponibilidadMapper.paraDto(guardada, slotsGenerados);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisponibilidadDtoResponse> obtenerDisponibilidadPorProfesional(Long profesionalId) {
        if (!profesionalRepositorio.existsById(profesionalId)) {
            throw new NoEncontradoExcepcion("Profesional no encontrado con ID: " + profesionalId);
        }

        return disponibilidadRepositorio.findByProfesionalIdAndActivoTrue(profesionalId)
                .stream()
                .map(disponibilidadMapper::paraDto)
                .toList();
    }

    @Override
    @Transactional
    public int generarSlotsProximos30Dias(Long profesionalId) {
        Profesional profesional = profesionalRepositorio.findById(profesionalId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + profesionalId));

        LocalDate hoy = LocalDate.now();
        List<Turno> nuevosSlots = new ArrayList<>();

        for (int i = 1; i <= 30; i++) {
            LocalDate fecha = hoy.plusDays(i);

            // Criterio de Aceptación: no generar cupos en días feriados
            if (feriadoServicio.esFeriado(fecha)) {
                log.debug("Omitiendo generación de slots para la fecha feriado: {}", fecha);
                continue;
            }

            List<DisponibilidadProfesional> disponibilidades =
                    disponibilidadRepositorio.findByProfesionalIdAndDiaSemanaAndActivoTrue(profesionalId, fecha.getDayOfWeek());

            for (DisponibilidadProfesional disp : disponibilidades) {
                LocalTime horaActual = disp.getHoraInicio();
                int intervalo = disp.getIntervaloMinutos();

                while (horaActual.plusMinutes(intervalo).compareTo(disp.getHoraFin()) <= 0) {
                    LocalDateTime fechaHoraSlot = LocalDateTime.of(fecha, horaActual);

                    // Criterio de Aceptación: no generar cupos en fechas previamente bloqueadas
                    boolean estaBloqueado = !bloqueoAgendaRepositorio.findBloqueosParaFechaHora(profesionalId, fechaHoraSlot).isEmpty();

                    // Criterio de Aceptación: evitar duplicidades de slots
                    boolean existeSlot = turnoRepositorio.existsByProfesionalIdAndFechaHora(profesionalId, fechaHoraSlot);

                    if (!estaBloqueado && !existeSlot) {
                        Turno slot = new Turno();
                        slot.setFechaHora(fechaHoraSlot);
                        slot.setEstado("DISPONIBLE");
                        slot.setProfesional(profesional);
                        slot.setPaciente(null);
                        nuevosSlots.add(slot);
                    }

                    horaActual = horaActual.plusMinutes(intervalo);
                }
            }
        }

        if (!nuevosSlots.isEmpty()) {
            turnoRepositorio.saveAll(nuevosSlots);
        }

        return nuevosSlots.size();
    }
}
