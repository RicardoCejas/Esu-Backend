package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.seguridad.Roles;
import com.SaludUnificada.Esu.servicio.IEspecialidadServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class ProfesionalMapper {

    @Autowired
    private IEspecialidadServicio especialidadServicio;

    @Autowired
    private EspecialidadMapper especialidadMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Profesional paraEntidad(ProfesionalDtoRequest dto) {
        Profesional profesional = new Profesional();
        profesional.setNombre(dto.getNombre());
        profesional.setApellido(dto.getApellido());
        profesional.setMatricula(dto.getMatricula());
        profesional.setTelefono(dto.getTelefono());
        profesional.setEstadoActivo(true);

        if (dto.getEspecialidadId() != null) {
            Especialidad especialidad = especialidadServicio.obtenerEntidadEspecialidadPorId(dto.getEspecialidadId());
            profesional.getEspecialidades().add(especialidad);
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

        if (!profesional.getEspecialidades().isEmpty()) {
            Especialidad especialidad = profesional.getEspecialidades().iterator().next();
            dto.setEspecialidad(especialidadMapper.paraDto(especialidad));
        }

        return dto;
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
