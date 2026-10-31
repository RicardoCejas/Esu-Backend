package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.ProfesionalMapper;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfesionalServicio implements IProfesionalServicio {

    private final ProfesionalRepositorio profesionalRepositorio;
    private final ProfesionalMapper profesionalMapper;
    private final TurnoRepositorio turnoRepositorio;

    @Autowired(required = false)
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired(required = false)
    private Clock clock;

    @Autowired(required = false)
    private ZoneId turnosZoneId;

    private LocalDateTime ahora() {
        ZoneId zone = turnosZoneId != null ? turnosZoneId : ZoneId.of("America/Argentina/Cordoba");
        Clock c = clock != null ? clock.withZone(zone) : Clock.system(zone);
        return LocalDateTime.now(c);
    }

    @Override
    @Transactional
    public ProfesionalDtoResponse crearProfesional(ProfesionalDtoRequest profesionalDto) {
        if (profesionalDto.getMatricula() == null || profesionalDto.getMatricula().isBlank()) {
            throw new IllegalArgumentException("La matricula del profesional es obligatoria");
        }
        String matriculaLimpia = profesionalDto.getMatricula().trim().toUpperCase();
        if (profesionalRepositorio.existsByMatricula(matriculaLimpia)) {
            throw new ReglaNegocioException("Ya existe un profesional registrado con la matricula: " + matriculaLimpia);
        }
        profesionalDto.setMatricula(matriculaLimpia);
        Profesional profesional = profesionalMapper.paraEntidad(profesionalDto);
        Profesional profesionalGuardado = profesionalRepositorio.save(profesional);
        return profesionalMapper.paraDto(profesionalGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfesionalDtoResponse obtenerProfesionalPorId(Long id) {
        Profesional profesional = obtenerEntidadProfesionalPorId(id);
        return profesionalMapper.paraDto(profesional);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfesionalDtoResponse> listarTodos() {
        return profesionalRepositorio.findAll().stream()
                .map(profesionalMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void eliminarProfesional(Long id) {
        if (profesionalRepositorio.existsById(id)) {
            profesionalRepositorio.deleteById(id);
        } else {
            throw new NoEncontradoExcepcion("Profesional no encontrado con ID: " + id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfesionalDtoResponse> filtrarPorEspecialidad(Long especialidadId) {
        return profesionalRepositorio.findByEspecialidades_Id(especialidadId).stream()
                .map(profesionalMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfesionalDtoResponse> buscarDisponibles(Long especialidadId, Long centroSaludId, Boolean soloConDisponibilidad) {
        List<Profesional> profesionales = profesionalRepositorio.buscarPrestadoresOptimizado(especialidadId, centroSaludId);
        LocalDateTime ahora = ahora();

        List<ProfesionalDtoResponse> resultado = new ArrayList<>();

        for (Profesional p : profesionales) {
            LocalDateTime proximoTurno = turnoRepositorio.findPrimerTurnoDisponible(p.getId(), ahora);

            if (Boolean.TRUE.equals(soloConDisponibilidad) && proximoTurno == null) {
                continue;
            }

            resultado.add(profesionalMapper.paraDto(p, proximoTurno));
        }

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Profesional obtenerEntidadProfesionalPorId(Long id) {
        return profesionalRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public ProfesionalDtoResponse cambiarEstadoActivo(Long id, boolean activo) {
        Profesional profesional = obtenerEntidadProfesionalPorId(id);
        profesional.setEstadoActivo(activo);
        if (!activo && profesional.getUsuario() != null) {
            profesional.getUsuario().revocarSesiones();
            if (usuarioRepositorio != null) {
                usuarioRepositorio.save(profesional.getUsuario());
            }
        }
        Profesional guardado = profesionalRepositorio.save(profesional);
        return profesionalMapper.paraDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Set<Long> obtenerCentrosDelProfesional(Long id) {
        Profesional profesional = profesionalRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + id));
        return new java.util.HashSet<>(profesional.getCentrosSaludIds());
    }

    @Override
    @Transactional
    public java.util.Set<Long> vincularCentro(Long id, Long centroId) {
        Profesional profesional = profesionalRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + id));
        profesional.getCentrosSaludIds().add(centroId);
        profesionalRepositorio.save(profesional);
        return new java.util.HashSet<>(profesional.getCentrosSaludIds());
    }

    @Override
    @Transactional
    public java.util.Set<Long> desvincularCentro(Long id, Long centroId) {
        Profesional profesional = profesionalRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + id));
        profesional.getCentrosSaludIds().remove(centroId);
        profesionalRepositorio.save(profesional);
        return new java.util.HashSet<>(profesional.getCentrosSaludIds());
    }
}
