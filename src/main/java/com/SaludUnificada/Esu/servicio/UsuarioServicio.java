package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.request.UsuarioPerfilOperativoDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.UsuarioMapper;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.SaludUnificada.Esu.repositorio.RefreshTokenRepositorio;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.HistoriaClinicaRepositorio;
import com.SaludUnificada.Esu.repositorio.EvolucionMedicaRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import com.SaludUnificada.Esu.repositorio.PermisoRepositorio;
import com.SaludUnificada.Esu.entidad.Permiso;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UsuarioServicio implements IUsuarioServicio {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private UsuarioMapper usuarioMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private RefreshTokenRepositorio refreshTokenRepositorio;

    @Autowired(required = false)
    private PacienteRepositorio pacienteRepositorio;

    @Autowired(required = false)
    private HistoriaClinicaRepositorio historiaClinicaRepositorio;

    @Autowired(required = false)
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    @Autowired(required = false)
    private RecetaDigitalRepositorio recetaDigitalRepositorio;

    @Autowired(required = false)
    private PermisoRepositorio permisoRepositorio;

    @Autowired(required = false)
    private ProfesionalRepositorio profesionalRepositorio;

    @Override
    public UsuarioDtoResponse crearUsuario(UsuarioDtoRequest usuarioDto) {
        if (usuarioRepositorio.existsByEmail(usuarioDto.getUsername())) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el email: " + usuarioDto.getUsername());
        }
        Usuario usuario = usuarioMapper.paraEntidad(usuarioDto);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        if (permisoRepositorio != null) {
            if (usuarioDto.getPermisos() != null && !usuarioDto.getPermisos().isEmpty()) {
                Set<Permiso> perms = permisoRepositorio.findByCodigoIn(new HashSet<>(usuarioDto.getPermisos()));
                usuario.setPermisos(perms);
            } else {
                usuario.setPermisos(obtenerPermisosDefectoPorRol(usuario.getRol()));
            }
        }
        Usuario usuarioGuardado = usuarioRepositorio.save(usuario);
        return usuarioMapper.paraDto(usuarioGuardado);
    }

    @Override
    public UsuarioDtoResponse obtenerUsuarioPorId(Long id) {
        Usuario usuario = obtenerEntidadUsuarioPorId(id);
        return usuarioMapper.paraDto(usuario);
    }

    @Override
    public List<UsuarioDtoResponse> listarTodos() {
        return usuarioRepositorio.findAll().stream()
                .map(usuarioMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UsuarioDtoResponse> listarPorCentro(Long centroSaludId) {
        if (centroSaludId == null) {
            return java.util.Collections.emptyList();
        }
        return usuarioRepositorio.findByCentroSaludId(centroSaludId).stream()
                .filter(u -> u.getRol() != null && !"ADMIN".equalsIgnoreCase(u.getRol()) && !"PACIENTE".equalsIgnoreCase(u.getRol()))
                .map(usuarioMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Usuario no encontrado con ID: " + id));

        if (pacienteRepositorio != null) {
            Optional<Paciente> pacienteOpt = pacienteRepositorio.findByUsuarioId(id);
            if (pacienteOpt.isPresent()) {
                Long pacId = pacienteOpt.get().getId();
                boolean tieneDatosClinicos = (historiaClinicaRepositorio != null && historiaClinicaRepositorio.countByPacienteId(pacId) > 0)
                        || (evolucionMedicaRepositorio != null && evolucionMedicaRepositorio.countByPacienteId(pacId) > 0)
                        || (recetaDigitalRepositorio != null && recetaDigitalRepositorio.countByPacienteId(pacId) > 0);
                if (tieneDatosClinicos) {
                    log.warn("Bloqueando eliminación física de Usuario ID={} vinculado a Paciente ID={} con registros clínicos protegidos por Ley 26.529. Aplicando baja lógica.", id, pacId);
                    usuario.setEstadoActivo(false);
                    usuario.revocarSesiones();
                    usuarioRepositorio.save(usuario);
                    throw new ReglaNegocioException("Por disposición de la Ley 26.529 de Derechos del Paciente, no es posible eliminar físicamente un usuario con registros clínicos asociados. La cuenta ha sido inhabilitada lógicamente.");
                }
            }
        }

        usuarioRepositorio.delete(usuario);
    }

    @Override
    public UsuarioDtoResponse findByUsername(String username) {
        Usuario usuario = usuarioRepositorio.findByEmail(username)
                .orElseThrow(() -> new NoEncontradoExcepcion("Usuario no encontrado con email: " + username));
        return usuarioMapper.paraDto(usuario);
    }

    @Override
    public List<UsuarioDtoResponse> findByActivo(boolean estado) {
        return usuarioRepositorio.findByEstadoActivo(estado).stream()
                .map(usuarioMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public Usuario obtenerEntidadUsuarioPorId(Long id) {
        return usuarioRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Usuario no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public UsuarioDtoResponse cambiarEstadoActivo(Long id, boolean activo) {
        Usuario usuario = obtenerEntidadUsuarioPorId(id);
        usuario.setEstadoActivo(activo);
        if (!activo) {
            usuario.revocarSesiones();
            if (refreshTokenRepositorio != null) {
                refreshTokenRepositorio.revocarTodosPorUsuarioId(usuario.getId());
            }
        }
        Usuario guardado = usuarioRepositorio.save(usuario);
        return usuarioMapper.paraDto(guardado);
    }

    @Override
    @Transactional
    public void revocarSesiones(Long id) {
        Usuario usuario = obtenerEntidadUsuarioPorId(id);
        usuario.revocarSesiones();
        usuarioRepositorio.save(usuario);
        if (refreshTokenRepositorio != null) {
            refreshTokenRepositorio.revocarTodosPorUsuarioId(id);
        }
    }

    @Override
    @Transactional
    public void revocarSesionesPorEmail(String email) {
        if (email == null || email.isBlank()) return;
        usuarioRepositorio.findByEmail(email.trim().toLowerCase()).ifPresent(u -> {
            u.revocarSesiones();
            usuarioRepositorio.save(u);
            if (refreshTokenRepositorio != null) {
                refreshTokenRepositorio.revocarTodosPorUsuarioId(u.getId());
            }
        });
    }

    @Override
    public List<String> obtenerPermisosUsuario(Long id) {
        Usuario usuario = obtenerEntidadUsuarioPorId(id);
        if (usuario.getPermisos() == null) return java.util.Collections.emptyList();
        return usuario.getPermisos().stream().map(Permiso::getCodigo).sorted().toList();
    }

    @Override
    @Transactional
    public UsuarioDtoResponse actualizarPermisosUsuario(Long id, List<String> codigosPermisos) {
        Usuario usuario = obtenerEntidadUsuarioPorId(id);
        if (permisoRepositorio != null) {
            Set<Permiso> perms = (codigosPermisos != null && !codigosPermisos.isEmpty())
                    ? permisoRepositorio.findByCodigoIn(new HashSet<>(codigosPermisos))
                    : new HashSet<>();
            usuario.setPermisos(perms);
            usuario.revocarSesiones(); // Fuerzo refresh de token para que tome los nuevos permisos
            Usuario actualizado = usuarioRepositorio.save(usuario);
            return usuarioMapper.paraDto(actualizado);
        }
        return usuarioMapper.paraDto(usuario);
    }

    @Override
    @Transactional
    public UsuarioDtoResponse actualizarRolYCentro(Long id, String nuevoRol, Long nuevoCentroSaludId, String nuevoCentroSaludNombre) {
        Usuario usuario = obtenerEntidadUsuarioPorId(id);
        if (nuevoRol != null && !nuevoRol.isBlank()) {
            usuario.setRol(nuevoRol.trim().toUpperCase());
        }
        if (nuevoCentroSaludId != null) {
            usuario.setCentroSaludId(nuevoCentroSaludId);
            usuario.setCentroSaludNombre(nuevoCentroSaludNombre);
        }
        usuario.revocarSesiones();
        Usuario actualizado = usuarioRepositorio.save(usuario);
        return usuarioMapper.paraDto(actualizado);
    }

    @Override
    @Transactional
    public UsuarioDtoResponse actualizarPerfilOperativo(Long id, UsuarioPerfilOperativoDtoRequest perfilDto) {
        Usuario usuario = obtenerEntidadUsuarioPorId(id);
        String nombreTrim = perfilDto.getNombre().trim();
        String apellidoTrim = perfilDto.getApellido().trim();
        String dniRaw = perfilDto.getDni() != null ? perfilDto.getDni().trim() : "";

        usuario.setNombre(nombreTrim);
        usuario.setApellido(apellidoTrim);
        usuario.setDni(dniRaw.isEmpty() ? null : dniRaw);

        if (profesionalRepositorio != null && usuario.getEmail() != null) {
            profesionalRepositorio.findByUsuarioEmail(usuario.getEmail()).ifPresent(profesional -> {
                profesional.setNombre(nombreTrim);
                profesional.setApellido(apellidoTrim);
                profesionalRepositorio.save(profesional);
            });
        }

        Usuario guardado = usuarioRepositorio.save(usuario);
        return usuarioMapper.paraDto(guardado);
    }

    private Set<Permiso> obtenerPermisosDefectoPorRol(String rol) {
        if (permisoRepositorio == null || rol == null) return new HashSet<>();
        String rolNorm = rol.toUpperCase().trim();
        if ("ADMIN".equals(rolNorm)) {
            return new HashSet<>(permisoRepositorio.findAll());
        } else if ("RECEPCIONISTA".equals(rolNorm)) {
            return permisoRepositorio.findByCodigoIn(Set.of(
                    "TURNOS_VER", "TURNOS_CREAR", "TURNOS_CANCELAR", "TURNOS_REAGENDAR",
                    "SOBRETURNOS_CREAR", "SALA_ESPERA_GESTIONAR", "PACIENTES_VER",
                    "PACIENTES_REGISTRAR", "CONSULTORIOS_VER", "CONSULTORIOS_ADMINISTRAR",
                    "MEDICOS_VER", "MEDICOS_ASIGNAR_CONSULTORIO", "REPORTES_VER",
                    "USUARIOS_ADMINISTRAR"
            ));
        } else if ("MEDICO".equals(rolNorm) || "PROFESIONAL".equals(rolNorm)) {
            return permisoRepositorio.findByCodigoIn(Set.of(
                    "TURNOS_VER", "PACIENTES_VER", "CONSULTORIOS_VER", "MEDICOS_VER"
            ));
        }
        return new HashSet<>();
    }
}
