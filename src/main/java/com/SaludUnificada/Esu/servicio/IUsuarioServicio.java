package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.entidad.Usuario;

import java.util.List;

public interface IUsuarioServicio {
    UsuarioDtoResponse crearUsuario(UsuarioDtoRequest usuario);
    UsuarioDtoResponse obtenerUsuarioPorId(Long id);
    List<UsuarioDtoResponse> listarTodos();
    void eliminarUsuario(Long id);
    UsuarioDtoResponse findByUsername(String username);
    List<UsuarioDtoResponse> findByActivo(boolean estado);
    Usuario obtenerEntidadUsuarioPorId(Long id);
}
