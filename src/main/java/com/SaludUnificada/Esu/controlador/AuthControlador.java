package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.JwtAuthDtoResponse;
import com.SaludUnificada.Esu.dto.auth.LoginDtoRequest;
import com.SaludUnificada.Esu.dto.auth.RegistroDtoRequest;
import com.SaludUnificada.Esu.dto.auth.ResetearPasswordDtoRequest;
import com.SaludUnificada.Esu.dto.auth.SolicitarRecuperacionDtoRequest;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import com.SaludUnificada.Esu.servicio.AuditoriaSeguridadServicio;
import com.SaludUnificada.Esu.servicio.EmailNotificacionServicio;
import com.SaludUnificada.Esu.servicio.RecuperacionPasswordServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthControlador {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RecuperacionPasswordServicio recuperacionPasswordServicio;
    private final AuditoriaSeguridadServicio auditoriaSeguridadServicio;
    private final EmailNotificacionServicio emailNotificacionServicio;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthDtoResponse> login(@Valid @RequestBody LoginDtoRequest loginDto) {
        if (auditoriaSeguridadServicio != null && auditoriaSeguridadServicio.estaBloqueadoPorIntentosFallidos(loginDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "La cuenta se encuentra temporalmente bloqueada por superar 5 intentos fallidos consecutivos en menos de 10 minutos. Por favor aguarde o recupere su clave.");
        }

        Usuario usuario = usuarioRepositorio.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!Boolean.TRUE.equals(usuario.getEstadoActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta se encuentra deshabilitada.");
        }

        if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Debe confirmar su dirección de correo electrónico antes de ingresar a la plataforma.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getEmail(),
                        loginDto.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generarToken(authentication);

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
        usuario.setFechaExpiracionVerificacion(LocalDateTime.now().plusHours(24));
        usuario.setTokenVerificacionUsado(false);

        Usuario guardado = usuarioRepositorio.save(usuario);

        if (pacienteRepositorio != null) {
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
            } catch (Exception ignored) {
            }
        }

        JwtAuthDtoResponse response = JwtAuthDtoResponse.builder()
                .accessToken(null)
                .tokenType("Bearer")
                .usuarioId(guardado.getId())
                .email(guardado.getEmail())
                .rol(guardado.getRol())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping({"/solicitar-recuperacion", "/forgot-password"})
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionDtoRequest request) {
        if (auditoriaSeguridadServicio != null && !auditoriaSeguridadServicio.puedeSolicitarRecuperacion(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Por favor espere un momento antes de solicitar un nuevo enlace de restablecimiento.");
        }
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
    @PreAuthorize("hasRole('ADMIN')")
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
            if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                email = auth.getName();
            }
        }

        if (email != null && !email.isBlank()) {
            if (auditoriaSeguridadServicio != null && !auditoriaSeguridadServicio.puedeReenviarVerificacion(email)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Por favor espere 60 segundos antes de solicitar otro correo de confirmación.");
            }
            Usuario usuario = usuarioRepositorio.findByEmail(email).orElse(null);
            if (usuario != null) {
                if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "La casilla de correo ya se encuentra verificada."
                    ));
                }
                String token = UUID.randomUUID().toString();
                usuario.setTokenVerificacion(token);
                usuario.setFechaExpiracionVerificacion(LocalDateTime.now().plusHours(24));
                usuario.setTokenVerificacionUsado(false);
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
                "message", "Si la cuenta existe y no está verificada, se ha enviado un nuevo enlace de confirmación."
        ));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verificarEmail(@RequestParam("token") String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de verificación no proporcionado");
        }

        Usuario usuario = usuarioRepositorio.findByTokenVerificacion(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace de verificación ha expirado o no es válido."));

        if (Boolean.TRUE.equals(usuario.getTokenVerificacionUsado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace de verificación ya ha sido utilizado.");
        }

        if (usuario.getFechaExpiracionVerificacion() != null && usuario.getFechaExpiracionVerificacion().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace de verificación ha expirado. Solicite un nuevo correo de confirmación.");
        }

        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacionUsado(true);
        usuario.setTokenVerificacion(null);
        usuarioRepositorio.save(usuario);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Correo electrónico verificado exitosamente. Ya puede iniciar sesión."
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> obtenerUsuarioActual(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        Usuario usuario = usuarioRepositorio.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return ResponseEntity.ok(Map.of(
                "id", usuario.getId(),
                "email", usuario.getEmail(),
                "rol", usuario.getRol() != null ? usuario.getRol() : "PACIENTE",
                "activo", Boolean.TRUE.equals(usuario.getEstadoActivo()),
                "verificado", Boolean.TRUE.equals(usuario.getEmailVerificado())
        ));
    }

    @GetMapping("/validar-token")
    public ResponseEntity<Map<String, Object>> validarToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valido", false));
        }
        String token = authHeader.substring(7);
        boolean valido = jwtTokenProvider.validarToken(token);
        String username = valido ? jwtTokenProvider.obtenerUsernameDelJwt(token) : null;
        return ResponseEntity.ok(Map.of(
                "valido", valido,
                "username", username != null ? username : ""
        ));
    }
}
