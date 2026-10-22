package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.entidad.CentroSalud;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.CentroSaludRepositorio;
import com.SaludUnificada.Esu.seguridad.Roles;
import com.SaludUnificada.Esu.servicio.IEspecialidadServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProfesionalMapper {

    @Autowired
    private IEspecialidadServicio especialidadServicio;

    @Autowired
    private EspecialidadMapper especialidadMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CentroSaludRepositorio centroSaludRepositorio;

    public Profesional paraEntidad(ProfesionalDtoRequest dto) {
        Profesional profesional = new Profesional();
        profesional.setNombre(dto.getNombre());
        profesional.setApellido(dto.getApellido());
        profesional.setMatricula(dto.getMatricula());
        profesional.setTelefono(dto.getTelefono());
        profesional.setEstadoActivo(true);
        profesional.setDiasAtencion(serializarDias(dto.getDiasAtencion()));
        profesional.setDuracionTurnoMin(dto.getDuracionTurnoMin() == null || dto.getDuracionTurnoMin() <= 0
                ? 20
                : dto.getDuracionTurnoMin());

        if (dto.getEspecialidadId() != null) {
            Especialidad especialidad = especialidadServicio.obtenerEntidadEspecialidadPorId(dto.getEspecialidadId());
            profesional.getEspecialidades().add(especialidad);
        }

        if (dto.getCentrosSaludIds() != null) {
            for (Long centroId : dto.getCentrosSaludIds()) {
                if (centroId != null) {
                    centroSaludRepositorio.findById(centroId).ifPresent(profesional.getCentrosSalud()::add);
                }
            }
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(resolverEmail(dto));
        usuario.setPassword(passwordEncoder.encode(resolverPassword(dto)));
        usuario.setRol(Roles.PROFESIONAL);
        usuario.setEstadoActivo(true);
        usuario.setFechaDeAlta(LocalDate.now());
        usuario.setProfesional(profesional);

        profesional.setUsuario(usuario);
        return profesional;
    }

    public ProfesionalDtoResponse paraDto(Profesional profesional) {
        ProfesionalDtoResponse dto = new ProfesionalDtoResponse();
        dto.setId(profesional.getId());
        dto.setNombre(profesional.getNombre());
        dto.setApellido(profesional.getApellido());
        dto.setMatricula(profesional.getMatricula());
        dto.setTelefono(profesional.getTelefono());
        if (profesional.getUsuario() != null) {
            dto.setEmail(profesional.getUsuario().getEmail());
        }

        if (!profesional.getEspecialidades().isEmpty()) {
            Especialidad especialidad = profesional.getEspecialidades().iterator().next();
            dto.setEspecialidad(especialidadMapper.paraDto(especialidad));
        }

        dto.setCentrosSaludIds(profesional.getCentrosSalud().stream()
                .map(centro -> String.valueOf(centro.getId()))
                .collect(Collectors.toList()));
        dto.setDiasAtencion(deserializarDias(profesional.getDiasAtencion()));
        dto.setDuracionTurnoMin(profesional.getDuracionTurnoMin() == null ? 20 : profesional.getDuracionTurnoMin());

        return dto;
    }

    private String serializarDias(List<String> dias) {
        if (dias == null || dias.isEmpty()) {
            return null;
        }
        return dias.stream()
                .filter(dia -> dia != null && !dia.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    private List<String> deserializarDias(String dias) {
        if (dias == null || dias.isBlank()) {
            return List.of();
        }
        return Arrays.stream(dias.split(","))
                .map(String::trim)
                .filter(dia -> !dia.isEmpty())
                .collect(Collectors.toList());
    }

    private String resolverEmail(ProfesionalDtoRequest dto) {
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            return dto.getEmail().trim().toLowerCase();
        }
        String base = dto.getMatricula() == null
                ? "profesional"
                : dto.getMatricula().toLowerCase().replaceAll("[^a-z0-9]", "");
        return (base.isBlank() ? "profesional" : base) + "@esu.local";
    }

    private String resolverPassword(ProfesionalDtoRequest dto) {
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            return dto.getPassword();
        }
        // Credencial temporal aleatoria: nunca se deriva de datos personales.
        return UUID.randomUUID() + "-Aa1!";
    }
}
