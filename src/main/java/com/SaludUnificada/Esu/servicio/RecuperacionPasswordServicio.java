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
            // Mitigación de user enumeration: respondemos sin revelar si el usuario existe o no
            log.info("Solicitud de recuperación para correo inexistente: {}", email);
            return;
        }

        Usuario usuario = optUsuario.get();
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
        log.info("Token de recuperación generado exitosamente para {}", usuario.getEmail());
    }

    @Transactional
    public void resetearPassword(ResetearPasswordDtoRequest request) {
        String tokenStr = request.getToken().trim();

        TokenRecuperacion token = tokenRepositorio.findByToken(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("El token de recuperación ingresado no es válido o no existe."));

        if (Boolean.TRUE.equals(token.getUsado())) {
            throw new IllegalStateException("El token de recuperación ya fue utilizado previamente.");
        }

        if (token.estaVencido()) {
            token.setUsado(true);
            tokenRepositorio.save(token);
            throw new IllegalStateException("El enlace de recuperación ha expirado (validez máxima 15 minutos). Por favor solicite uno nuevo.");
        }

        Usuario usuario = token.getUsuario();
        if (usuario == null) {
            throw new NoEncontradoExcepcion("Usuario no asociado al token de recuperación.");
        }

        usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepositorio.save(usuario);

        // Invalidación inmediata del token
        token.setUsado(true);
        tokenRepositorio.save(token);

        log.info("Contraseña actualizada exitosamente para el usuario {}", usuario.getEmail());
    }
}
