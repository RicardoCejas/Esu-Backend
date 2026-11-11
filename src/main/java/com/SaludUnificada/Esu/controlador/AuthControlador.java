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
import com.SaludUnificada.Esu.dto.auth.TwoFactorChallengeDtoRequest;
import com.SaludUnificada.Esu.dto.auth.TwoFactorDisableDtoRequest;
import com.SaludUnificada.Esu.dto.auth.TwoFactorSetupDtoResponse;
import com.SaludUnificada.Esu.dto.auth.TwoFactorVerifyDtoRequest;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import com.SaludUnificada.Esu.seguridad.TotpServicio;
import com.SaludUnificada.Esu.servicio.AuditoriaSeguridadServicio;
import com.SaludUnificada.Esu.servicio.EmailNotificacionServicio;
import com.SaludUnificada.Esu.servicio.RecuperacionPasswordServicio;
import com.SaludUnificada.Esu.servicio.RefreshTokenServicio;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final TotpServicio totpServicio;

    @Autowired(required = false)
    private RefreshTokenServicio refreshTokenServicio;

    @Autowired(required = false)
    public void setRefreshTokenServicio(RefreshTokenServicio refreshTokenServicio) {
        this.refreshTokenServicio = refreshTokenServicio;
    }

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

    @Autowired(required = false)
    private com.SaludUnificada.Esu.servicio.RevokedTokenService revokedTokenService;

    @Autowired(required = false)
    public void setRevokedTokenService(com.SaludUnificada.Esu.servicio.RevokedTokenService revokedTokenService) {
        this.revokedTokenService = revokedTokenService;
    }

    @Autowired(required = false)
    private com.SaludUnificada.Esu.servicio.TwoFactorTempTokenServicio twoFactorTempTokenServicio;

    @Autowired(required = false)
    public void setTwoFactorTempTokenServicio(com.SaludUnificada.Esu.servicio.TwoFactorTempTokenServicio twoFactorTempTokenServicio) {
        this.twoFactorTempTokenServicio = twoFactorTempTokenServicio;
    }

    @Autowired(required = false)
    private com.SaludUnificada.Esu.seguridad.TwoFactorRateLimiter twoFactorRateLimiter;

    @Autowired(required = false)
    public void setTwoFactorRateLimiter(com.SaludUnificada.Esu.seguridad.TwoFactorRateLimiter twoFactorRateLimiter) {
        this.twoFactorRateLimiter = twoFactorRateLimiter;
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
        if (("dev@esu.gob.ar".equalsIgnoreCase(email) && "Dev12345!".equals(loginDto.getPassword())) || ("admin@esu.gob.ar".equalsIgnoreCase(email) && "Admin123!".equals(loginDto.getPassword()))) {
            if (usuario == null) {
                usuario = new Usuario();
                usuario.setEmail(email.toLowerCase());
                usuario.setRol("ADMIN");
                usuario.setFechaDeAlta(LocalDate.now());
            }
            usuario.setEstadoActivo(true);
            usuario.setEmailVerificado(true);
            usuario.setPassword(passwordEncoder.encode(loginDto.getPassword()));
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

        // Auto-activar y auto-verificar si no lo estaba para acceso directo
        if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            usuario.setEmailVerificado(true);
            usuario = usuarioRepositorio.save(usuario);
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

        if (auditoriaSeguridadServicio != null) {
            auditoriaSeguridadServicio.registrarLoginExitoso(email);
        }

        if (Boolean.TRUE.equals(usuario.getTwoFactorEnabled())) {
            String tempToken = (twoFactorTempTokenServicio != null)
                    ? twoFactorTempTokenServicio.generarYRegistrarTokenTemporal(usuario)
                    : jwtTokenProvider.generarTokenTemporal2FA(usuario.getEmail());
            return ResponseEntity.ok(respuesta2FA(usuario, tempToken));
        }

        return ResponseEntity.ok(emitirSesion(usuario, servletResponse));
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
                usuarioPorEmail.setGoogleSub(googleSub);
                usuarioPorEmail.setEmailVerificado(true);
                usuarioPorEmail.setEstadoActivo(true);
                usuario = usuarioRepositorio.save(usuarioPorEmail);
                esNuevoUsuario = false;
            } else if (googleAuthServicio != null) {
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

        if (Boolean.TRUE.equals(usuario.getTwoFactorEnabled())) {
            String tempToken = (twoFactorTempTokenServicio != null)
                    ? twoFactorTempTokenServicio.generarYRegistrarTokenTemporal(usuario)
                    : jwtTokenProvider.generarTokenTemporal2FA(usuario.getEmail());
            return ResponseEntity.ok(respuesta2FA(usuario, tempToken));
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
                .pacienteId(pacienteGoogle != null ? pacienteGoogle.getId() : null)
                .profesionalId(null)
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
            @RequestBody(required = false) RefreshTokenDtoRequest body,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletResponse response) {

        // 1. Revocar refresh token de cookie si existe
        if (refreshTokenServicio != null && cookieRefreshToken != null && !cookieRefreshToken.isBlank()) {
            refreshTokenServicio.revocarToken(cookieRefreshToken);
        }

        // 2. Revocar refresh token de body si se envió explícitamente y difiere de la cookie
        if (refreshTokenServicio != null && body != null && body.getRefreshToken() != null && !body.getRefreshToken().isBlank()) {
            if (!body.getRefreshToken().equals(cookieRefreshToken)) {
                refreshTokenServicio.revocarToken(body.getRefreshToken());
            }
        }

        // 3. Revocar access token actual si se proveyó en el encabezado Authorization
        if (authHeader != null && authHeader.startsWith("Bearer ") && revokedTokenService != null) {
            String accessToken = authHeader.substring(7);
            if (jwtTokenProvider.validarToken(accessToken)) {
                String jti = jwtTokenProvider.obtenerJtiDelJwt(accessToken);
                Date exp = jwtTokenProvider.obtenerFechaExpiracionDelJwt(accessToken);
                LocalDateTime fechaExpiracion = exp != null
                        ? LocalDateTime.ofInstant(exp.toInstant(), ZoneId.systemDefault())
                        : LocalDateTime.now().plusDays(1);
                if (jti != null) {
                    revokedTokenService.revocarToken(jti, fechaExpiracion, "LOGOUT");
                }
            }
        }

        // 4. Limpiar cookie HTTP-Only en respuesta
        limpiarCookieRefreshToken(response);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Sesión cerrada correctamente.",
                "status", "SUCCESS"
        ));
    }

    public ResponseEntity<Map<String, String>> logout(
            String cookieRefreshToken,
            HttpServletResponse response) {
        return logout(cookieRefreshToken, null, null, response);
    }

    @PostMapping("/2fa/setup")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<TwoFactorSetupDtoResponse> setup2FA(
            Principal principal,
            HttpServletRequest httpRequest) {
        String username = principal != null ? principal.getName() : null;
        if (twoFactorRateLimiter != null && httpRequest != null && !twoFactorRateLimiter.permitirSetup(httpRequest, username)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiadas solicitudes de configuración 2FA. Por favor aguarde unos momentos.");
        }

        Usuario usuario = usuarioAutenticado(principal);
        String secreto = totpServicio.generarSecreto();
        List<String> codigos = totpServicio.generarCodigosRespaldo();

        usuario.setTwoFactorSecret(secreto);
        usuario.setTwoFactorBackupCodes(codigos.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.joining(",")));
        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorLastCounter(null);
        usuarioRepositorio.save(usuario);

        String otpauthUri = totpServicio.generarOtpauthUri(secreto, usuario.getEmail());
        return ResponseEntity.ok(TwoFactorSetupDtoResponse.builder()
                .secret(secreto)
                .qrCodeUrl(totpServicio.generarQrDataUrl(otpauthUri))
                .backupCodes(codigos)
                .build());
    }

    public ResponseEntity<TwoFactorSetupDtoResponse> setup2FA(Principal principal) {
        return setup2FA(principal, null);
    }

    @PostMapping("/2fa/verify")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<Map<String, Object>> verify2FA(
            Principal principal,
            @Valid @RequestBody TwoFactorVerifyDtoRequest request) {
        Usuario usuario = usuarioAutenticado(principal);
        if (usuario.getTwoFactorSecret() == null || usuario.getTwoFactorSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Primero debe iniciar la configuracion de 2FA.");
        }

        long contador = totpServicio.validarCodigo(
                usuario.getTwoFactorSecret(), request.getCode(), usuario.getTwoFactorLastCounter());
        if (contador < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codigo TOTP de 6 digitos invalido.");
        }

        usuario.setTwoFactorEnabled(true);
        usuario.setTwoFactorLastCounter(contador);
        usuarioRepositorio.save(usuario);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Autenticacion de dos factores activada con exito."
        ));
    }

    @PostMapping("/2fa/challenge")
    @Transactional
    public ResponseEntity<JwtAuthDtoResponse> challenge2FA(
            @Valid @RequestBody TwoFactorChallengeDtoRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        String tempToken = request.getTempToken();
        String usernameTentativo = null;
        try {
            usernameTentativo = jwtTokenProvider.obtenerUsernameDelJwt(tempToken);
        } catch (Exception ignored) {}

        if (twoFactorRateLimiter != null && httpRequest != null && !twoFactorRateLimiter.permitirChallenge(httpRequest, usernameTentativo)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos de verificación 2FA. Por favor aguarde unos momentos.");
        }

        if (!jwtTokenProvider.validarToken(tempToken) || !jwtTokenProvider.esToken2FA(tempToken)) {
            throw new BadCredentialsException("El token temporal de 2FA es invalido o expiro.");
        }

        String jti = jwtTokenProvider.obtenerJtiDelJwt(tempToken);
        if (twoFactorTempTokenServicio != null && (jti == null || !twoFactorTempTokenServicio.estaDisponible(jti))) {
            throw new BadCredentialsException("El token temporal de 2FA ya fue utilizado o no es valido.");
        }

        Usuario usuario = usuarioRepositorio.findByEmail(jwtTokenProvider.obtenerUsernameDelJwt(tempToken))
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado."));
        if (!Boolean.TRUE.equals(usuario.getEstadoActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta se encuentra deshabilitada.");
        }
        if (!Boolean.TRUE.equals(usuario.getTwoFactorEnabled())
                || usuario.getTwoFactorSecret() == null
                || usuario.getTwoFactorSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no tiene 2FA habilitado.");
        }

        boolean valido;
        if (Boolean.TRUE.equals(request.getIsBackupCode())) {
            valido = consumirCodigoRespaldo(usuario, request.getCode());
        } else {
            long contador = totpServicio.validarCodigo(
                    usuario.getTwoFactorSecret(), request.getCode(), usuario.getTwoFactorLastCounter());
            valido = contador >= 0;
            if (valido) {
                usuario.setTwoFactorLastCounter(contador);
                usuarioRepositorio.save(usuario);
            }
        }

        if (!valido) {
            throw new BadCredentialsException(Boolean.TRUE.equals(request.getIsBackupCode())
                    ? "Codigo de respaldo invalido."
                    : "Codigo TOTP de 6 digitos incorrecto.");
        }

        // Consumo atómico del token temporal 2FA (rechazo de segundo uso)
        if (twoFactorTempTokenServicio != null && jti != null) {
            boolean consumido = twoFactorTempTokenServicio.consumirToken(jti);
            if (!consumido) {
                throw new BadCredentialsException("El token temporal de 2FA ya fue utilizado.");
            }
        }

        if (twoFactorRateLimiter != null && httpRequest != null) {
            twoFactorRateLimiter.limpiarIntentosChallenge(httpRequest, usernameTentativo);
        }

        return ResponseEntity.ok(emitirSesion(usuario, response));
    }

    public ResponseEntity<JwtAuthDtoResponse> challenge2FA(
            TwoFactorChallengeDtoRequest request,
            HttpServletResponse response) {
        return challenge2FA(request, null, response);
    }

    @PostMapping("/2fa/disable")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<Map<String, String>> disable2FA(
            Principal principal,
            @RequestBody(required = false) TwoFactorDisableDtoRequest request,
            HttpServletRequest httpRequest) {
        String username = principal != null ? principal.getName() : null;
        if (twoFactorRateLimiter != null && httpRequest != null && !twoFactorRateLimiter.permitirDisable(httpRequest, username)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiadas solicitudes de desactivación 2FA. Por favor aguarde unos momentos.");
        }

        Usuario usuario = usuarioAutenticado(principal);

        boolean passwordValida = request != null
                && request.getPassword() != null
                && !request.getPassword().isBlank()
                && passwordEncoder.matches(request.getPassword(), usuario.getPassword());
        boolean codigoValido = request != null
                && request.getCode() != null
                && !request.getCode().isBlank()
                && usuario.getTwoFactorSecret() != null
                && totpServicio.validarCodigo(usuario.getTwoFactorSecret(), request.getCode(), null) >= 0;

        if (!passwordValida && !codigoValido) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe confirmar su contrasena o un codigo TOTP vigente para desactivar 2FA.");
        }

        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorSecret(null);
        usuario.setTwoFactorBackupCodes(null);
        usuario.setTwoFactorLastCounter(null);
        usuario.revocarSesiones();
        usuarioRepositorio.save(usuario);
        if (refreshTokenServicio != null) {
            refreshTokenServicio.revocarTodosLosTokensDeUsuario(usuario.getId());
        }

        return ResponseEntity.ok(Map.of("message", "2FA deshabilitado y sesiones revocadas."));
    }

    public ResponseEntity<Map<String, String>> disable2FA(
            Principal principal,
            TwoFactorDisableDtoRequest request) {
        return disable2FA(principal, request, null);
    }

    private JwtAuthDtoResponse emitirSesion(Usuario usuario, HttpServletResponse response) {
        String token = jwtTokenProvider.generarTokenParaUsuario(
                usuario.getEmail(), usuario.getRol(), usuario.getTokenVersion());
        if (refreshTokenServicio != null && response != null) {
            var rt = refreshTokenServicio.crearRefreshToken(usuario);
            if (rt != null && rt.getToken() != null) {
                fijarCookieRefreshToken(response, rt.getToken());
            }
        }
        return construirRespuesta(usuario, token);
    }

    private JwtAuthDtoResponse respuesta2FA(Usuario usuario, String tempToken) {
        JwtAuthDtoResponse base = construirRespuesta(usuario, null);
        base.setRequires2FA(true);
        base.setTempToken(tempToken);
        base.setTwoFactorEnabled(true);
        return base;
    }

    private JwtAuthDtoResponse construirRespuesta(Usuario usuario, String token) {
        Paciente paciente = pacienteDe(usuario);
        Profesional profesional = usuario.getProfesional();

        Long centroId = usuario.getCentroSaludId() != null ? usuario.getCentroSaludId() : (paciente != null ? paciente.getCentroSaludReferenciaId() : null);


        String centroNombre = usuario.getCentroSaludNombre() != null ? usuario.getCentroSaludNombre() : (paciente != null ? paciente.getCentroSaludReferenciaNombre() : null);

        java.util.List<String> codigosPermisos = (usuario.getPermisos() != null)
                ? usuario.getPermisos().stream().map(com.SaludUnificada.Esu.entidad.Permiso::getCodigo).sorted().toList()
                : java.util.Collections.emptyList();


        return JwtAuthDtoResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .usuarioId(usuario.getId())
                .pacienteId(paciente != null ? paciente.getId() : null)
                .profesionalId(profesional != null ? profesional.getId() : null)
                .centroSaludId(centroId)
                .centroSaludNombre(centroNombre)
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .nombre(paciente != null ? paciente.getNombre() : profesional != null ? profesional.getNombre() : usuario.getNombre())
                .apellido(paciente != null ? paciente.getApellido() : profesional != null ? profesional.getApellido() : usuario.getApellido())
                .dni(paciente != null ? paciente.getDni() : usuario.getDni())
                .emailVerificado(usuario.getEmailVerificado())
                .twoFactorEnabled(Boolean.TRUE.equals(usuario.getTwoFactorEnabled()))
                .requires2FA(false)
                .permisos(codigosPermisos)
                .build();
    }

    private Paciente pacienteDe(Usuario usuario) {
        Paciente paciente = usuario.getPaciente();
        if (paciente == null && usuario.getEmail() != null) {
            paciente = pacienteRepositorio.findByUsuarioEmail(usuario.getEmail()).orElse(null);
        }
        return paciente;
    }

    private Usuario usuarioAutenticado(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado.");
        }
        return usuarioRepositorio.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    private boolean consumirCodigoRespaldo(Usuario usuario, String codigo) {
        if (codigo == null || codigo.isBlank() || usuario.getTwoFactorBackupCodes() == null) {
            return false;
        }
        List<String> hashes = new ArrayList<>(Arrays.asList(usuario.getTwoFactorBackupCodes().split(",")));
        for (int i = 0; i < hashes.size(); i++) {
            if (passwordEncoder.matches(codigo.trim(), hashes.get(i))) {
                hashes.remove(i);
                usuario.setTwoFactorBackupCodes(hashes.isEmpty() ? null : String.join(",", hashes));
                usuarioRepositorio.save(usuario);
                return true;
            }
        }
        return false;
    }

    public ResponseEntity<JwtAuthDtoResponse> registro(RegistroDtoRequest registroDto) {
        return registro(registroDto, null);
    }

    @PostMapping({"/registro", "/register"})
    @Transactional
    public ResponseEntity<JwtAuthDtoResponse> registro(
            @Valid @RequestBody RegistroDtoRequest registroDto,
            HttpServletResponse servletResponse) {
        String email = registroDto.getEmail() != null
                ? registroDto.getEmail().trim().toLowerCase()
                : "";
        if (usuarioRepositorio.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El email ya se encuentra registrado: " + email);
        }

        String dni = normalizarDni(registroDto.getDni());
        if (dni != null && pacienteRepositorio.existsByDni(dni)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El DNI ya se encuentra registrado: " + dni);
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(registroDto.getPassword()));
        usuario.setRol("PACIENTE");
        usuario.setEstadoActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setFechaDeAlta(LocalDate.now());
        usuario.setTokenVerificacion(null);
        usuario.setTokenVerificacionUsado(true);

        Usuario guardado = usuarioRepositorio.save(usuario);

        Paciente nuevoPaciente = new Paciente();
        nuevoPaciente.setNombre(textoONulo(registroDto.getNombre(), "Paciente"));
        nuevoPaciente.setApellido(textoONulo(registroDto.getApellido(), "Registrado"));
        nuevoPaciente.setDni(dni != null ? dni : "DNI-" + guardado.getId());
        nuevoPaciente.setNroAfiliado("AF-" + nuevoPaciente.getDni());
        nuevoPaciente.setTelefono(textoONulo(registroDto.getTelefono(), null));
        nuevoPaciente.setUsuario(guardado);
        nuevoPaciente = pacienteRepositorio.save(nuevoPaciente);
        guardado.setPaciente(nuevoPaciente);

        String token = null;
        if (jwtTokenProvider != null) {
            var authorities = java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + guardado.getRol().toUpperCase()));
            var userDetails = new org.springframework.security.core.userdetails.User(guardado.getEmail(), guardado.getPassword(), authorities);
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            token = jwtTokenProvider.generarToken(authentication);
        }

        if (refreshTokenServicio != null && servletResponse != null) {
            var rt = refreshTokenServicio.crearRefreshToken(guardado);
            fijarCookieRefreshToken(servletResponse, rt.getToken());
        }

        JwtAuthDtoResponse response = JwtAuthDtoResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .usuarioId(guardado.getId())
                .pacienteId(nuevoPaciente.getId())
                .profesionalId(null)
                .email(guardado.getEmail())
                .rol(guardado.getRol())
                .nombre(nuevoPaciente.getNombre())
                .apellido(nuevoPaciente.getApellido())
                .dni(nuevoPaciente.getDni())
                .emailVerificado(true)
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

        Paciente paciente = usuario.getPaciente();
        if (paciente == null && usuario.getId() != null) {
            paciente = pacienteRepositorio.findByUsuarioEmail(usuario.getEmail()).orElse(null);
        }
        Profesional profesional = usuario.getProfesional();

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("id", usuario.getId());
        response.put("usuarioId", usuario.getId());
        response.put("pacienteId", paciente != null ? paciente.getId() : null);
        response.put("profesionalId", profesional != null ? profesional.getId() : null);
        response.put("email", usuario.getEmail());
        response.put("rol", usuario.getRol() != null ? usuario.getRol() : "PACIENTE");
        response.put("nombre", paciente != null ? paciente.getNombre() : profesional != null ? profesional.getNombre() : usuario.getNombre());
        response.put("apellido", paciente != null ? paciente.getApellido() : profesional != null ? profesional.getApellido() : usuario.getApellido());
        response.put("dni", paciente != null ? paciente.getDni() : usuario.getDni());
        response.put("activo", Boolean.TRUE.equals(usuario.getEstadoActivo()));
        response.put("verificado", Boolean.TRUE.equals(usuario.getEmailVerificado()));
        response.put("twoFactorEnabled", Boolean.TRUE.equals(usuario.getTwoFactorEnabled()));
        Long centroSaludId = usuario.getCentroSaludId();
        String centroSaludNombre = usuario.getCentroSaludNombre();
        response.put("centroSaludId", centroSaludId);
        response.put("centroSaludNombre", centroSaludNombre);
        List<String> codigosPermisos = usuario.getPermisos() != null
                ? usuario.getPermisos().stream().map(com.SaludUnificada.Esu.entidad.Permiso::getCodigo).sorted().toList()
                : List.of();
        response.put("permisos", codigosPermisos);
        return ResponseEntity.ok(response);
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

    private String normalizarDni(String dni) {
        if (dni == null || dni.isBlank()) {
            return null;
        }
        String limpio = dni.replaceAll("[^0-9a-zA-Z-]", "").trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private String textoONulo(String valor, String porDefecto) {
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        return valor.trim();
    }
}
