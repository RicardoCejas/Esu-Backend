package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.JwtAuthDtoResponse;
import com.SaludUnificada.Esu.dto.auth.LoginDtoRequest;
import com.SaludUnificada.Esu.dto.auth.RegistroDtoRequest;
import com.SaludUnificada.Esu.dto.auth.ResetearPasswordDtoRequest;
import com.SaludUnificada.Esu.dto.auth.SolicitarRecuperacionDtoRequest;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import com.SaludUnificada.Esu.servicio.AuditoriaSeguridadServicio;
import com.SaludUnificada.Esu.servicio.RecuperacionPasswordServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthControlador {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RecuperacionPasswordServicio recuperacionPasswordServicio;

    @Autowired
    private AuditoriaSeguridadServicio auditoriaSeguridadServicio;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthDtoResponse> login(@Valid @RequestBody LoginDtoRequest loginDto) {
        // HU-100: Chequeo de bloqueo temporal por intentos fallidos consecutivos
        if (auditoriaSeguridadServicio.estaBloqueadoPorIntentosFallidos(loginDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "La cuenta se encuentra temporalmente bloqueada por superar 5 intentos fallidos consecutivos en menos de 10 minutos. Por favor aguarde o recupere su clave.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getEmail(),
                        loginDto.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generarToken(authentication);

        Usuario usuario = usuarioRepositorio.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        JwtAuthDtoResponse response = JwtAuthDtoResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping({"/registro", "/register"})
    public ResponseEntity<JwtAuthDtoResponse> registro(@Valid @RequestBody RegistroDtoRequest registroDto) {
        if (usuarioRepositorio.existsByEmail(registroDto.getEmail())) {
            throw new IllegalArgumentException("El email ya se encuentra registrado: " + registroDto.getEmail());
        }

        String rol = (registroDto.getRol() != null && !registroDto.getRol().isBlank())
                ? registroDto.getRol().toUpperCase()
                : "PACIENTE";

        Usuario usuario = new Usuario();
        usuario.setEmail(registroDto.getEmail());
        usuario.setPassword(passwordEncoder.encode(registroDto.getPassword()));
        usuario.setRol(rol);
        usuario.setEstadoActivo(true);
        usuario.setFechaDeAlta(LocalDate.now());

        Usuario guardado = usuarioRepositorio.save(usuario);

        String token = jwtTokenProvider.generarTokenParaUsuario(guardado.getEmail(), guardado.getRol());

        JwtAuthDtoResponse response = JwtAuthDtoResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .usuarioId(guardado.getId())
                .email(guardado.getEmail())
                .rol(guardado.getRol())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // HU-99: Flujo Criptográfico de Recuperación de Contraseña
    @PostMapping({"/solicitar-recuperacion", "/forgot-password"})
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionDtoRequest request) {
        recuperacionPasswordServicio.solicitarRecuperacion(request);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Si el correo electrónico se encuentra registrado, recibirá un enlace temporal para restablecer su contraseña en los próximos minutos."
        ));
    }

    @PostMapping({"/resetear-password", "/reset-password"})
    public ResponseEntity<Map<String, String>> resetearPassword(@Valid @RequestBody ResetearPasswordDtoRequest request) {
        recuperacionPasswordServicio.resetearPassword(request);
        return ResponseEntity.ok(Map.of(
                "mensaje", "La contraseña ha sido actualizada exitosamente. Ya puede iniciar sesión con sus nuevas credenciales."
        ));
    }
}
