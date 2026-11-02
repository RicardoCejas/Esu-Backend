package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.CambiarPasswordDtoRequest;
import com.SaludUnificada.Esu.dto.auth.GoogleIdTokenPayload;
import com.SaludUnificada.Esu.dto.auth.GoogleLoginDtoRequest;
import com.SaludUnificada.Esu.dto.auth.JwtAuthDtoResponse;
import com.SaludUnificada.Esu.dto.auth.LoginDtoRequest;
import com.SaludUnificada.Esu.dto.auth.RefreshTokenDtoRequest;
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
import com.SaludUnificada.Esu.servicio.RefreshTokenServicio;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthControlador.class);

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RecuperacionPasswordServicio recuperacionPasswordServicio;
    private final AuditoriaSeguridadServicio auditoriaSeguridadServicio;
    private final EmailNotificacionServicio emailNotificacionServicio;

    @Autowired(required = false)
    private RefreshTokenServicio refreshTokenServicio;

    @Autowired(required = false)
    private com.SaludUnificada.Esu.servicio.GoogleTokenVerifierService googleTokenVerifierService;

    @Autowired(required = false)
    public void setGoogleTokenVerifierService(com.SaludUnificada.Esu.servicio.GoogleTokenVerifierService googleTokenVerifierService) {
        this.googleTokenVerifierService = googleTokenVerifierService;
    }

    @Autowired(required = false)
    private com.SaludUnificada.Esu.servicio.GoogleAuthServicio googleAuthServicio;

    @Autowired(required = false)
    public void setGoogleAuthServicio(com.SaludUnificada.Esu.servicio.GoogleAuthServicio googleAuthServicio) {
        this.googleAuthServicio = googleAuthServicio;
    }

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    private boolean esProduccion() {
        return "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);
    }

    @Value("${app.auth.cookie.same-site:}")
    private String cookieSameSiteConfig;

    @Value("${app.auth.cookie.secure:}")
    private String cookieSecureConfig;

    private boolean esCookieSegura() {
        if ("true".equalsIgnoreCase(cookieSecureConfig)) return true;
        if ("false".equalsIgnoreCase(cookieSecureConfig)) return false;
        return esProduccion();
    }

    private String obtenerSameSite() {
        if (cookieSameSiteConfig != null && !cookieSameSiteConfig.isBlank()) {
            return cookieSameSiteConfig.trim();
        }
        return esProduccion() ? "None" : "Lax";
    }

    private void fijarCookieRefreshToken(HttpServletResponse response, String refreshTokenStr) {
        if (response == null || refreshTokenStr == null) return;
        boolean secure = esCookieSegura();
        String sameSite = obtenerSameSite();
        if ("None".equalsIgnoreCase(sameSite)) {
            secure = true;
        }
        ResponseCookie cookie = ResponseCookie.from("esu_refresh_token", refreshTokenStr)
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")
                .maxAge(7 * 24 * 3600)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void limpiarCookieRefreshToken(HttpServletResponse response) {
        if (response == null) return;
        boolean secure = esCookieSegura();
        String sameSite = obtenerSameSite();
        if ("None".equalsIgnoreCase(sameSite)) {
            secure = true;
        }
        ResponseCookie cookie = ResponseCookie.from("esu_refresh_token", "")
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")
                .maxAge(0)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthDtoResponse> login(
            @Valid @RequestBody LoginDtoRequest loginDto,
            HttpServletResponse servletResponse) {
        return ejecutarLogin(loginDto, servletResponse);
    }

    public ResponseEntity<JwtAuthDtoResponse> login(LoginDtoRequest loginDto) {
        return ejecutarLogin(loginDto, null);
    }

    private ResponseEntity<JwtAuthDtoResponse> ejecutarLogin(LoginDtoRequest loginDto, HttpServletResponse servletResponse) {
        String email = loginDto.getEmail() != null ? loginDto.getEmail().trim().toLowerCase() : "";
        if (auditoriaSeguridadServicio != null && auditoriaSeguridadServicio.estaBloqueadoPorIntentosFallidos(email)) {
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "La cuenta se encuentra temporalmente bloqueada por superar intentos fallidos consecutivos. Por favor aguarde o recupere su clave.");
        }

        Usuario usuario = usuarioRepositorio.findByEmail(email).orElse(null);

        // Habilitar acceso de desarrollo para pruebas locales con dev@esu.gob.ar
        if (!esProduccion() && "dev@esu.gob.ar".equalsIgnoreCase(email) && "Dev12345!".equals(loginDto.getPassword())) {
            if (usuario == null) {
                usuario = new Usuario();
                usuario.setEmail("dev@esu.gob.ar");
                usuario.setRol("ADMIN");
                usuario.setFechaDeAlta(LocalDate.now());
            }
            usuario.setEstadoActivo(true);
            usuario.setEmailVerificado(true);
            usuario.setPassword(passwordEncoder.encode("Dev12345!"));
            usuario = usuarioRepositorio.save(usuario);
        }
        if (usuario == null) {
            if (auditoriaSeguridadServicio != null) {
                auditoriaSeguridadServicio.registrarLoginFallido(email, "Cuenta no existente");
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas.");
        }

        if (!Boolean.TRUE.equals(usuario.getEstadoActivo())) {
            if (auditoriaSeguridadServicio != null) {
                auditoriaSeguridadServicio.registrarLoginFallido(email, "Cuenta inactiva");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta se encuentra deshabilitada.");
        }

        if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            if (auditoriaSeguridadServicio != null) {
                auditoriaSeguridadServicio.registrarLoginFallido(email, "Email no verificado");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Debe confirmar su dirección de correo electrónico antes de ingresar a la plataforma.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginDto.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            if (auditoriaSeguridadServicio != null) {
                auditoriaSeguridadServicio.registrarLoginFallido(email, "Contraseña incorrecta");
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas.");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generarToken(authentication);

        if (auditoriaSeguridadServicio != null) {
            auditoriaSeguridadServicio.registrarLoginExitoso(email);
        }

        if (refreshTokenServicio != null && servletResponse != null) {
            var rt = refreshTokenServicio.crearRefreshToken(usuario);
            fijarCookieRefreshToken(servletResponse, rt.getToken());
        }

        Paciente paciente = usuario.getPaciente();
        if (paciente == null && usuario.getId() != null) {
            paciente = pacienteRepositorio.findByUsuarioEmail(usuario.getEmail()).orElse(null);
        }

        JwtAuthDtoResponse response = JwtAuthDtoResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .nombre(paciente != null ? paciente.getNombre() : null)
                .apellido(paciente != null ? paciente.getApellido() : null)
                .dni(paciente != null ? paciente.getDni() : null)
                .emailVerificado(usuario.getEmailVerificado())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<JwtAuthDtoResponse> loginConGoogle(
            @Valid @RequestBody GoogleLoginDtoRequest request,
            HttpServletResponse servletResponse) {

        if (googleTokenVerifierService == null) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Servicio Google OAuth no disponible.");
        }

        GoogleIdTokenPayload payload;
        try {
            payload = googleTokenVerifierService.verificarToken(request.getIdToken());
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        String googleSub = payload.getSub();
        String email = payload.getEmail();

        // 1. Identificar por sub estable de Google
        Usuario usuario = usuarioRepositorio.findByGoogleSub(googleSub).orElse(null);
        boolean esNuevoUsuario = false;

        if (usuario == null) {
            esNuevoUsuario = true;
            // 2. Si no existe por sub, revisar si ya existe una cuenta local con ese email
            Usuario usuarioPorEmail = usuarioRepositorio.findByEmail(email).orElse(null);
            if (usuarioPorEmail != null) {
                if (auditoriaSeguridadServicio != null) {
                    auditoriaSeguridadServicio.registrarLoginFallido(email, "Intento de login Google en cuenta local existente no vinculada");
                }
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El correo electrónico ya se encuentra registrado mediante cuenta local. Por motivos de seguridad, inicie sesión con su contraseña tradicional.");
            }

            // 3. Crear usuario nuevo con rol público por defecto (PACIENTE) de forma transaccional con su perfil de Paciente
            if (googleAuthServicio != null) {
                try {
                    usuario = googleAuthServicio.registrarUsuarioConPaciente(payload);
                } catch (Exception e) {
                    log.error("Fallo al registrar usuario Google con perfil de paciente: {}", e.getMessage());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error al crear el perfil de paciente para la cuenta de Google. Intente nuevamente.");
                }
            } else {
                usuario = new Usuario();
                usuario.setEmail(email);
                usuario.setGoogleSub(googleSub);
                usuario.setAuthProvider("GOOGLE");
                usuario.setRol("PACIENTE");
                usuario.setEstadoActivo(true);
                usuario.setEmailVerificado(true);
                usuario.setFechaDeAlta(LocalDate.now());
                usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                usuario = usuarioRepositorio.save(usuario);
            }
        }

        if (!Boolean.TRUE.equals(usuario.getEstadoActivo())) {
            if (auditoriaSeguridadServicio != null) {
                auditoriaSeguridadServicio.registrarLoginFallido(email, "Cuenta Google deshabilitada");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta se encuentra deshabilitada.");
        }

        var authorities = java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + usuario.getRol().toUpperCase()));
        var userDetails = new org.springframework.security.core.userdetails.User(usuario.getEmail(), usuario.getPassword(), authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generarToken(authentication);

        if (auditoriaSeguridadServicio != null) {
            auditoriaSeguridadServicio.registrarLoginExitoso(email);
        }

        if (refreshTokenServicio != null && servletResponse != null) {
            var rt = refreshTokenServicio.crearRefreshToken(usuario);
            fijarCookieRefreshToken(servletResponse, rt.getToken());
        }

        Paciente pacienteGoogle = usuario.getPaciente();
        if (pacienteGoogle == null && usuario.getId() != null) {
            pacienteGoogle = pacienteRepositorio.findByUsuarioEmail(usuario.getEmail()).orElse(null);
        }

        if (esNuevoUsuario && emailNotificacionServicio != null) {
            try {
                String nombreDestinatario = pacienteGoogle != null && pacienteGoogle.getNombre() != null
                        ? pacienteGoogle.getNombre()
                        : (payload.getGivenName() != null ? payload.getGivenName() : email.split("@")[0]);
                emailNotificacionServicio.enviarBienvenida(usuario.getEmail(), nombreDestinatario);
            } catch (Exception e) {
                log.warn("No se pudo despachar el email de bienvenida para el usuario Google {}: {}", email, e.getMessage());
            }
        }

        JwtAuthDtoResponse response = JwtAuthDtoResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .nombre(pacienteGoogle != null ? pacienteGoogle.getNombre() : null)
                .apellido(pacienteGoogle != null ? pacienteGoogle.getApellido() : null)
                .dni(pacienteGoogle != null ? pacienteGoogle.getDni() : null)
                .emailVerificado(usuario.getEmailVerificado())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthDtoResponse> refresh(
            @CookieValue(name = "esu_refresh_token", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenDtoRequest body,
            HttpServletResponse response) {

        String token = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (body != null ? body.getRefreshToken() : null);

        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Refresh token ausente o inválido.");
        }

        if (refreshTokenServicio == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de sesiones no disponible.");
        }

        RefreshTokenServicio.RefreshResult result = refreshTokenServicio.rotarRefreshToken(token);
        fijarCookieRefreshToken(response, result.newRefreshTokenStr());
        return ResponseEntity.ok(result.authResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = "esu_refresh_token", required = false) String cookieRefreshToken,
            HttpServletResponse response) {
        if (refreshTokenServicio != null && cookieRefreshToken != null && !cookieRefreshToken.isBlank()) {
            refreshTokenServicio.revocarToken(cookieRefreshToken);
        }
        limpiarCookieRefreshToken(response);
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente."));
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
        String tokenVerificacionHash = RefreshTokenServicio.hashToken(tokenVerificacion);
        usuario.setTokenVerificacion(tokenVerificacionHash);
        usuario.setEmailVerificado(false);
        usuario.setFechaExpiracionVerificacion(LocalDateTime.now().plusHours(24));
        usuario.setTokenVerificacionUsado(false);

        Usuario guardado = usuarioRepositorio.save(usuario);

        if (pacienteRepositorio != null) {
            try {
                if (!pacienteRepositorio.existsByUsuarioEmail(guardado.getEmail())) {
                    Paciente nuevoPaciente = new Paciente();
                    String nom = (registroDto.getNombre() != null && !registroDto.getNombre().isBlank()) ? registroDto.getNombre().trim() : "Paciente";
                    String ape = (registroDto.getApellido() != null && !registroDto.getApellido().isBlank()) ? registroDto.getApellido().trim() : "Registrado";
                    nuevoPaciente.setNombre(nom);
                    nuevoPaciente.setApellido(ape);
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

    @PostMapping({"/cambiar-password", "/change-password"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> cambiarPassword(
            Principal principal,
            @Valid @RequestBody CambiarPasswordDtoRequest request) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado.");
        }

        Usuario usuario = usuarioRepositorio.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        boolean esCuentaGooglePura = "GOOGLE".equalsIgnoreCase(usuario.getAuthProvider())
                && (request.getPasswordActual() == null || request.getPasswordActual().isBlank());

        if (!esCuentaGooglePura) {
            if (request.getPasswordActual() == null || request.getPasswordActual().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar su contraseña actual.");
            }
            if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta.");
            }
        }

        usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        usuario.setFechaActualizacionPassword(LocalDateTime.now());
        usuario.revocarSesiones();
        usuarioRepositorio.save(usuario);

        if (refreshTokenServicio != null) {
            refreshTokenServicio.revocarTodosLosTokensDeUsuario(usuario.getId());
        }

        return ResponseEntity.ok(Map.of(
                "mensaje", "Contraseña actualizada exitosamente."
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
                String tokenHash = RefreshTokenServicio.hashToken(token);
                usuario.setTokenVerificacion(tokenHash);
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

        String tokenHash = RefreshTokenServicio.hashToken(token.trim());
        Usuario usuario = usuarioRepositorio.findByTokenVerificacion(tokenHash)
                .or(() -> usuarioRepositorio.findByTokenVerificacion(token.trim()))
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
