package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.auth.ResetearPasswordDtoRequest;
import com.SaludUnificada.Esu.dto.auth.SolicitarRecuperacionDtoRequest;
import com.SaludUnificada.Esu.entidad.TokenRecuperacion;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.TokenRecuperacionRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecuperacionPasswordServicio {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionPasswordServicio.class);
    private static final int EXPIRACION_MINUTOS = 15;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private TokenRecuperacionRepositorio tokenRepositorio;

    @Autowired
    private EmailNotificacionServicio emailServicio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void solicitarRecuperacion(SolicitarRecuperacionDtoRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<Usuario> optUsuario = usuarioRepositorio.findByEmail(email);

        if (optUsuario.isEmpty()) {
            // MitigaciÃ³n de user enumeration: respondemos sin revelar si el usuario existe o no
            log.info("Solicitud de recuperaciÃ³n para correo inexistente: {}", email);
            return;
        }

        Usuario usuario = optUsuario.get();

        // Invalidar tokens previos activos del usuario
        List<TokenRecuperacion> previos = tokenRepositorio.findByUsuarioIdAndUsadoFalse(usuario.getId());
        for (TokenRecuperacion prev : previos) {
            prev.setUsado(true);
        }
        if (!previos.isEmpty()) {
            tokenRepositorio.saveAll(previos);
        }

        String tokenStr = UUID.randomUUID().toString();

        TokenRecuperacion token = new TokenRecuperacion();
        token.setToken(tokenStr);
        token.setUsuario(usuario);
        token.setFechaExpiracion(LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS));
        token.setUsado(false);

        tokenRepositorio.save(token);

        String nombre = (usuario.getPaciente() != null)
                ? usuario.getPaciente().getNombre()
                : (usuario.getProfesional() != null ? usuario.getProfesional().getNombre() : usuario.getEmail());

        emailServicio.enviarTokenRecuperacion(usuario.getEmail(), nombre, tokenStr);
        log.info("Token de recuperaciÃ³n generado exitosamente para {}", usuario.getEmail());
    }

    @Transactional
    public void resetearPassword(ResetearPasswordDtoRequest request) {
        String tokenStr = request.getToken().trim();

        TokenRecuperacion token = tokenRepositorio.findByToken(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("El token de recuperaciÃ³n ingresado no es vÃ¡lido o no existe."));

        if (Boolean.TRUE.equals(token.getUsado())) {
            throw new IllegalStateException("El token de recuperaciÃ³n ya fue utilizado previamente.");
        }

        if (token.estaVencido()) {
            token.setUsado(true);
            tokenRepositorio.save(token);
            throw new IllegalStateException("El enlace de recuperaciÃ³n ha expirado (validez mÃ¡xima 15 minutos). Por favor solicite uno nuevo.");
        }

        Usuario usuario = token.getUsuario();
        if (usuario == null) {
            throw new NoEncontradoExcepcion("Usuario no asociado al token de recuperaciÃ³n.");
        }

        usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        usuario.setFechaActualizacionPassword(LocalDateTime.now());
        usuario.revocarSesiones(); // Invalida todas las sesiones previas incrementando tokenVersion
        usuarioRepositorio.save(usuario);

        // InvalidaciÃ³n inmediata del token
        token.setUsado(true);
        tokenRepositorio.save(token);

        log.info("ContraseÃ±a actualizada exitosamente para el usuario {}", usuario.getEmail());
    }
}
