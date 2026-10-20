package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.mapper.UsuarioMapper;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioServicio implements IUsuarioServicio {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private UsuarioMapper usuarioMapper;

    @Override
    public UsuarioDtoResponse crearUsuario(UsuarioDtoRequest usuarioDto) {
        if (usuarioDto.getUsername() == null || usuarioDto.getUsername().isEmpty()) {
            throw new IllegalArgumentException("El email del usuario es obligatorio");
        }
        if (usuarioDto.getPassword() == null || usuarioDto.getPassword().isEmpty()) {
            throw new IllegalArgumentException("La contraseña del usuario es obligatoria");
        }
        Usuario usuario = usuarioMapper.paraEntidad(usuarioDto);
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
            throw new RuntimeException("Usuario no encontrado con ID: " + id);
        }
    }

    @Override
    public UsuarioDtoResponse findByUsername(String username) {
        Usuario usuario = usuarioRepositorio.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + username));
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
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }
}
