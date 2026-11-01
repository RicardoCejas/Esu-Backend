package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.UsuarioMapper;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.SaludUnificada.Esu.repositorio.RefreshTokenRepositorio;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public UsuarioDtoResponse crearUsuario(UsuarioDtoRequest usuarioDto) {
        if (usuarioRepositorio.existsByEmail(usuarioDto.getUsername())) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el email: " + usuarioDto.getUsername());
        }
        Usuario usuario = usuarioMapper.paraEntidad(usuarioDto);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
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
    public void eliminarUsuario(Long id) {
        if (usuarioRepositorio.existsById(id)) {
            usuarioRepositorio.deleteById(id);
        } else {
            throw new NoEncontradoExcepcion("Usuario no encontrado con ID: " + id);
        }
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
}
