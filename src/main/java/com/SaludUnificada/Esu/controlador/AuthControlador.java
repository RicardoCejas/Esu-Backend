package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.servicio.EmailNotificacionServicio;

import com.SaludUnificada.Esu.dto.auth.JwtAuthDtoResponse;
import com.SaludUnificada.Esu.dto.auth.LoginDtoRequest;
import com.SaludUnificada.Esu.dto.auth.RegistroDtoRequest;
import com.SaludUnificada.Esu.dto.auth.ResetearPasswordDtoRequest;
import com.SaludUnificada.Esu.dto.auth.SolicitarRecuperacionDtoRequest;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.entidad.Paciente;
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
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthControlador {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired(required = false)
    private PacienteRepositorio pacienteRepositorio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RecuperacionPasswordServicio recuperacionPasswordServicio;

    @Autowired
    private AuditoriaSeguridadServicio auditoriaSeguridadServicio;
    @Autowired(required = false)
    private EmailNotificacionServicio emailNotificacionServicio;


    @PostMapping("/login")
    public ResponseEntity<JwtAuthDtoResponse> login(@Valid @RequestBody LoginDtoRequest loginDto) {
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

        String rol = "PACIENTE";

        Usuario usuario = new Usuario();
        usuario.setEmail(registroDto.getEmail());
        usuario.setPassword(passwordEncoder.encode(registroDto.getPassword()));
        usuario.setRol(rol);
        usuario.setEstadoActivo(true);
        usuario.setFechaDeAlta(LocalDate.now());
        String tokenVerificacion = UUID.randomUUID().toString();
        usuario.setTokenVerificacion(tokenVerificacion);
        usuario.setEmailVerificado(false);


        Usuario guardado = usuarioRepositorio.save(usuario);

        if ("PACIENTE".equalsIgnoreCase(rol) && pacienteRepositorio != null) {
            try {
                if (!pacienteRepositorio.existsByUsuarioEmail(guardado.getEmail())) {
                    Paciente nuevoPaciente = new Paciente();
                    nuevoPaciente.setNombre("Paciente");
                    nuevoPaciente.setApellido("Registrado");
                    nuevoPaciente.setDni("DNI-" + guardado.getId());
                    nuevoPaciente.setNroAfiliado("AF-" + guardado.getId());
                    nuevoPaciente.setUsuario(guardado);
                    pacienteRepositorio.save(nuevoPaciente);
                }
            } catch (Exception ignored) {
            }
        }

        
        if (emailNotificacionServicio != null) {
            try {
                emailNotificacionServicio.enviarEmailVerificacion(guardado.getEmail(), guardado.getEmail().split("@")[0], tokenVerificacion);
                emailNotificacionServicio.enviarBienvenida(guardado.getEmail(), guardado.getEmail().split("@")[0]);
            } catch (Exception ignored) {
            }
        }

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

    @GetMapping("/test-email")
    public ResponseEntity<Map<String, String>> testEmail(@RequestParam("destinatario") String destinatario) {
        if (emailNotificacionServicio != null) {
            emailNotificacionServicio.enviarBienvenida(destinatario, "Usuario de Prueba");
            return ResponseEntity.ok(Map.of(
                    "estado", "OK",
                    "mensaje", "Correo de prueba enviado a " + destinatario
            ));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "estado", "ERROR",
                "mensaje", "Servicio de email no disponible"
        ));
    }


    @PostMapping("/verify-email/resend")
    public ResponseEntity<Map<String, Object>> reenviarVerificacionEmail(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                email = auth.getName();
            }
        }

        if (email != null && !email.isBlank()) {
            Usuario usuario = usuarioRepositorio.findByEmail(email).orElse(null);
            if (usuario != null) {
                String token = UUID.randomUUID().toString();
                usuario.setTokenVerificacion(token);
                usuarioRepositorio.save(usuario);
                if (emailNotificacionServicio != null) {
                    try {
                        emailNotificacionServicio.enviarEmailVerificacion(usuario.getEmail(), usuario.getEmail().split("@")[0], token);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Se ha enviado un nuevo enlace de confirmacion a tu casilla de correo."
        ));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verificarEmail(@RequestParam("token") String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de verificacion no proporcionado");
        }

        Usuario usuario = usuarioRepositorio.findByTokenVerificacion(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace de verificacion ha expirado o no es valido."));

        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacion(null);
        usuarioRepositorio.save(usuario);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Correo electronico verificado exitosamente."
        ));
    }

}
