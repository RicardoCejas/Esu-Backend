package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.ProfesionalMapper;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private IEspecialidadServicio especialidadServicio;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

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

        if (profesionalDto.getEmail() != null && !profesionalDto.getEmail().isBlank() && usuarioRepositorio != null) {
            String emailNormalizado = profesionalDto.getEmail().trim().toLowerCase();
            if (usuarioRepositorio.findByEmail(emailNormalizado).isPresent()) {
                throw new ReglaNegocioException("Ya existe una cuenta registrada con el email: " + emailNormalizado);
            }
            Usuario usuario = new Usuario();
            usuario.setEmail(emailNormalizado);
            usuario.setNombre(profesional.getNombre());
            usuario.setApellido(profesional.getApellido());
            if (profesionalDto.getCentrosSaludIds() != null && !profesionalDto.getCentrosSaludIds().isEmpty()) {
                usuario.setCentroSaludId(profesionalDto.getCentrosSaludIds().iterator().next());
            }
            String rawPass = (profesionalDto.getPassword() != null && !profesionalDto.getPassword().isBlank())
                    ? profesionalDto.getPassword()
                    : matriculaLimpia;
            usuario.setPassword(passwordEncoder != null ? passwordEncoder.encode(rawPass) : rawPass);
            usuario.setRol("MEDICO");
            usuario.setEstadoActivo(true);
            usuario.setEmailVerificado(true);
            usuario.setFechaDeAlta(LocalDate.now());
            profesional.setUsuario(usuario);
        }

        Profesional profesionalGuardado = profesionalRepositorio.save(profesional);
        return profesionalMapper.paraDto(profesionalGuardado);
    }

    @Override
    @Transactional
    public ProfesionalDtoResponse actualizarProfesional(Long id, ProfesionalDtoRequest profesionalDto) {
        Profesional profesional = obtenerEntidadProfesionalPorId(id);

        if (profesionalDto.getNombre() != null && !profesionalDto.getNombre().isBlank()) {
            profesional.setNombre(profesionalDto.getNombre().trim());
        }
        if (profesionalDto.getApellido() != null && !profesionalDto.getApellido().isBlank()) {
            profesional.setApellido(profesionalDto.getApellido().trim());
        }
        if (profesionalDto.getMatricula() != null && !profesionalDto.getMatricula().isBlank()) {
            String nuevaMatricula = profesionalDto.getMatricula().trim().toUpperCase();
            if (!profesional.getMatricula().equalsIgnoreCase(nuevaMatricula) && profesionalRepositorio.existsByMatricula(nuevaMatricula)) {
                throw new ReglaNegocioException("Ya existe otro profesional con la matricula: " + nuevaMatricula);
            }
            profesional.setMatricula(nuevaMatricula);
        }
        if (profesionalDto.getTelefono() != null) {
            profesional.setTelefono(profesionalDto.getTelefono().trim());
        }
        if (profesionalDto.getEspecialidadId() != null && especialidadServicio != null) {
            Especialidad especialidad = especialidadServicio.obtenerEntidadEspecialidadPorId(profesionalDto.getEspecialidadId());
            profesional.setEspecialidades(new HashSet<>(Set.of(especialidad)));
        }
        if (profesionalDto.getCentrosSaludIds() != null && !profesionalDto.getCentrosSaludIds().isEmpty()) {
            profesional.setCentrosSaludIds(new HashSet<>(profesionalDto.getCentrosSaludIds()));
        }
        if (profesional.getUsuario() != null) {
            profesional.getUsuario().setNombre(profesional.getNombre());
            profesional.getUsuario().setApellido(profesional.getApellido());
        }

        Profesional guardado = profesionalRepositorio.save(profesional);
        return profesionalMapper.paraDto(guardado);
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
    public List<ProfesionalDtoResponse> filtrarPorCentro(Long centroId) {
        return profesionalRepositorio.findAll().stream()
                .filter(p -> p.getCentrosSaludIds() != null && p.getCentrosSaludIds().contains(centroId))
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
        if (turnoRepositorio != null && turnoRepositorio.existsTurnosActivosFuturosDeProfesionalEnCentro(id, centroId, ahora())) {
            throw new ReglaNegocioException("No se puede desvincular al profesional del centro porque posee turnos programados pendientes. Debe cancelarlos o reasignarlos previamente.");
        }
        profesional.getCentrosSaludIds().remove(centroId);
        profesionalRepositorio.save(profesional);
        return new java.util.HashSet<>(profesional.getCentrosSaludIds());
    }
}
