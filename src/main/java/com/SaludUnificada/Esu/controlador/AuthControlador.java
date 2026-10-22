package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.ForgotPasswordDtoRequest;
import com.SaludUnificada.Esu.dto.auth.JwtAuthDtoResponse;
import com.SaludUnificada.Esu.dto.auth.LoginDtoRequest;
import com.SaludUnificada.Esu.dto.auth.RegistroDtoRequest;
import com.SaludUnificada.Esu.dto.auth.ResetPasswordDtoRequest;
import com.SaludUnificada.Esu.dto.auth.TwoFactorChallengeDtoRequest;
import com.SaludUnificada.Esu.dto.auth.TwoFactorSetupDtoResponse;
import com.SaludUnificada.Esu.dto.auth.TwoFactorVerifyDtoRequest;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import com.SaludUnificada.Esu.seguridad.RevokedTokenService;
import com.SaludUnificada.Esu.seguridad.Roles;
import com.SaludUnificada.Esu.seguridad.TotpServicio;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthControlador {

    private static final Logger log = LoggerFactory.getLogger(AuthControlador.class);

    /**
     * Issue #117: roles que un visitante anónimo puede solicitar en el autorregistro.
     * Los roles privilegiados (ADMIN, PROFESIONAL, RECEPCIONISTA) sólo se asignan desde
     * un endpoint protegido por ADMIN.
     */
    private static final Set<String> ROLES_AUTORREGISTRO = Set.of("PACIENTE");

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RevokedTokenService revokedTokenService;

    @Autowired
    private TotpServicio totpServicio;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.auth.log-dev-tokens:true}")
    private boolean logDevTokens;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthDtoResponse> login(
            @Valid @RequestBody LoginDtoRequest loginDto,
            HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizarEmail(loginDto.getEmail()),
                        loginDto.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        Usuario usuario = buscarPorEmail(loginDto.getEmail());

        if (Boolean.TRUE.equals(usuario.getTwoFactorEnabled())) {
            String tempToken = jwtTokenProvider.generarTokenTemporal2FA(usuario.getEmail());
            return ResponseEntity.ok(respuesta2FA(usuario, tempToken));
        }

        return ResponseEntity.ok(emitirSesion(usuario, response));
    }

    @PostMapping({"/registro", "/register"})
    public ResponseEntity<JwtAuthDtoResponse> registro(
            @Valid @RequestBody RegistroDtoRequest registroDto,
            HttpServletResponse response) {

        String email = normalizarEmail(registroDto.getEmail());
        if (usuarioRepositorio.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya se encuentra registrado: " + email);
        }

        String rol = "PACIENTE";
        if (registroDto.getRol() != null && !registroDto.getRol().isBlank()) {
            String solicitado = registroDto.getRol().trim().toUpperCase();
            if (!ROLES_AUTORREGISTRO.contains(solicitado)) {
                throw new IllegalArgumentException(
                        "El rol '" + solicitado + "' no está permitido para autorregistro.");
            }
            rol = solicitado;
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(registroDto.getPassword()));
        usuario.setRol(rol);
        usuario.setEstadoActivo(true);
        usuario.setFechaDeAlta(LocalDate.now());
        usuario.setTwoFactorEnabled(false);
        usuario.setEmailVerificado(false);

        String tokenVerificacion = generarTokenAleatorio();
        usuario.setEmailVerificationToken(hashToken(tokenVerificacion));

        Usuario guardado = usuarioRepositorio.save(usuario);
        registrarTokenDeDesarrollo("verificacion de email", email, tokenVerificacion);

        return new ResponseEntity<>(emitirSesion(guardado, response), HttpStatus.CREATED);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthDtoResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        Usuario usuario = usuarioDesdeRefresh(request);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(emitirSesion(usuario, response));
    }

    /**
     * Issue #117: cierre de sesión. Invalida el token presentado hasta su expiración natural
     * agregando su {@code jti} a la lista de revocados y limpia la cookie de refresh.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletResponse response) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                if (jwtTokenProvider.validarToken(token) && jwtTokenProvider.esTokenDeAcceso(token)) {
                    revokedTokenService.revocar(
                            jwtTokenProvider.obtenerJti(token),
                            jwtTokenProvider.obtenerExpiracion(token));
                }
            } catch (Exception ignored) {
                // Token inválido o malformado: el logout es idempotente.
            }
        }
        limpiarRefreshCookie(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------------
    // 2FA: TOTP real con secreto por usuario, QR y codigos de respaldo
    // ------------------------------------------------------------------------

    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupDtoResponse> setup2FA(Authentication authentication) {
        Usuario usuario = usuarioAutenticado(authentication);

        String secreto = totpServicio.generarSecreto();
        List<String> codigos = totpServicio.generarCodigosRespaldo();

        usuario.setTwoFactorSecret(secreto);
        usuario.setTwoFactorBackupCodes(codigos.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.joining(",")));
        usuario.setTwoFactorEnabled(false);
        usuarioRepositorio.save(usuario);

        String otpauthUri = totpServicio.generarOtpauthUri(secreto, usuario.getEmail());
        TwoFactorSetupDtoResponse respuesta = TwoFactorSetupDtoResponse.builder()
                .secret(secreto)
                .qrCodeUrl(totpServicio.generarQrDataUrl(otpauthUri))
                .backupCodes(codigos)
                .build();

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<Map<String, Object>> verify2FA(
            Authentication authentication,
            @Valid @RequestBody TwoFactorVerifyDtoRequest dto) {

        Usuario usuario = usuarioAutenticado(authentication);
        if (usuario.getTwoFactorSecret() == null || usuario.getTwoFactorSecret().isBlank()) {
            throw new IllegalArgumentException("Primero debe iniciar la configuracion de 2FA.");
        }
        if (!totpServicio.validar(usuario.getTwoFactorSecret(), dto.getCode())) {
            throw new IllegalArgumentException("Codigo TOTP de 6 digitos invalido.");
        }

        usuario.setTwoFactorEnabled(true);
        usuarioRepositorio.save(usuario);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("success", true);
        respuesta.put("message", "Autenticacion de dos factores activada con exito.");
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/2fa/challenge")
    public ResponseEntity<JwtAuthDtoResponse> challenge2FA(
            @Valid @RequestBody TwoFactorChallengeDtoRequest dto,
            HttpServletResponse response) {

        String tempToken = dto.getTempToken();
        if (tempToken == null || !jwtTokenProvider.validarToken(tempToken)
                || !jwtTokenProvider.esToken2FA(tempToken)) {
            throw new BadCredentialsException("El token temporal de 2FA es invalido o expiro.");
        }

        Usuario usuario = buscarPorEmail(jwtTokenProvider.obtenerUsernameDelJwt(tempToken));
        if (!Boolean.TRUE.equals(usuario.getTwoFactorEnabled())
                || usuario.getTwoFactorSecret() == null
                || usuario.getTwoFactorSecret().isBlank()) {
            throw new IllegalArgumentException("El usuario no tiene 2FA habilitado.");
        }

        boolean valido = Boolean.TRUE.equals(dto.getIsBackupCode())
                ? consumirCodigoRespaldo(usuario, dto.getCode())
                : totpServicio.validar(usuario.getTwoFactorSecret(), dto.getCode());

        if (!valido) {
            throw new BadCredentialsException(Boolean.TRUE.equals(dto.getIsBackupCode())
                    ? "Codigo de respaldo invalido."
                    : "Codigo TOTP de 6 digitos incorrecto.");
        }

        return ResponseEntity.ok(emitirSesion(usuario, response));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Map<String, String>> disable2FA(Authentication authentication) {
        Usuario usuario = usuarioAutenticado(authentication);
        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorSecret(null);
        usuario.setTwoFactorBackupCodes(null);
        usuarioRepositorio.save(usuario);

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("message", "2FA deshabilitado.");
        return ResponseEntity.ok(respuesta);
    }

    // ------------------------------------------------------------------------
    // Recuperacion de contrasena y verificacion de email
    // ------------------------------------------------------------------------

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordDtoRequest dto) {
        String email = normalizarEmail(dto.getEmail());
        Optional<Usuario> usuarioOpt = usuarioRepositorio.findByEmail(email);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String token = generarTokenAleatorio();
            usuario.setResetPasswordToken(hashToken(token));
            usuario.setResetPasswordExpiry(LocalDateTime.now().plusMinutes(15));
            usuarioRepositorio.save(usuario);
            registrarTokenDeDesarrollo("recuperacion de contrasena", email, token);
        }

        // Feedback no revelador para mitigar enumeracion de cuentas.
        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("message", "Si el correo electronico se encuentra registrado en nuestro sistema, "
                + "recibiras un enlace con las instrucciones para restablecer tu contrasena.");
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordDtoRequest dto) {
        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("La contrasena debe tener un minimo de 8 caracteres.");
        }

        Usuario usuario = usuarioRepositorio.findByResetPasswordToken(hashToken(dto.getToken()))
                .orElseThrow(() -> new IllegalArgumentException("El token de recuperacion es invalido."));

        if (usuario.getResetPasswordExpiry() == null
                || usuario.getResetPasswordExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El token de recuperacion expiro.");
        }

        usuario.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        usuario.setResetPasswordToken(null);
        usuario.setResetPasswordExpiry(null);
        usuarioRepositorio.save(usuario);

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("message", "Contrasena actualizada exitosamente.");
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam("token") String token) {
        Usuario usuario = usuarioRepositorio.findByEmailVerificationToken(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException("El token de verificacion es invalido."));

        usuario.setEmailVerificado(true);
        usuario.setEmailVerificationToken(null);
        usuarioRepositorio.save(usuario);

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("message", "Correo electronico verificado exitosamente.");
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/verify-email/resend")
    public ResponseEntity<Map<String, String>> resendVerifyEmail(@RequestBody Map<String, String> payload) {
        String email = normalizarEmail(payload.getOrDefault("email", ""));
        if (!email.isBlank()) {
            usuarioRepositorio.findByEmail(email).ifPresent(usuario -> {
                if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
                    String token = generarTokenAleatorio();
                    usuario.setEmailVerificationToken(hashToken(token));
                    usuarioRepositorio.save(usuario);
                    registrarTokenDeDesarrollo("verificacion de email", email, token);
                }
            });
        }

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("message", "Se ha enviado un nuevo enlace de confirmacion a tu casilla de correo.");
        return ResponseEntity.ok(respuesta);
    }

    // ------------------------------------------------------------------------
    // Helpers de sesion, cookies y tokens
    // ------------------------------------------------------------------------

    private JwtAuthDtoResponse emitirSesion(Usuario usuario, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.generarTokenParaUsuario(usuario.getEmail(), usuario.getRol());
        String refreshToken = jwtTokenProvider.generarRefreshToken(usuario.getEmail());
        agregarRefreshCookie(response, refreshToken);

        return JwtAuthDtoResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .rol(Roles.requerido(usuario.getRol()))
                .twoFactorEnabled(Boolean.TRUE.equals(usuario.getTwoFactorEnabled()))
                .emailVerified(Boolean.TRUE.equals(usuario.getEmailVerificado()))
                .requires2FA(false)
                .build();
    }

    private JwtAuthDtoResponse respuesta2FA(Usuario usuario, String tempToken) {
        return JwtAuthDtoResponse.builder()
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .rol(Roles.requerido(usuario.getRol()))
                .twoFactorEnabled(true)
                .emailVerified(Boolean.TRUE.equals(usuario.getEmailVerificado()))
                .requires2FA(true)
                .tempToken(tempToken)
                .build();
    }

    private Usuario usuarioDesdeRefresh(HttpServletRequest request) {
        String refreshToken = leerCookie(request, "refreshToken");
        try {
            if (refreshToken != null && jwtTokenProvider.validarToken(refreshToken)
                    && jwtTokenProvider.esTokenRefresh(refreshToken)) {
                return usuarioActivo(jwtTokenProvider.obtenerUsernameDelJwt(refreshToken));
            }

            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String accessToken = authorization.substring(7);
                if (jwtTokenProvider.validarToken(accessToken) && jwtTokenProvider.esTokenDeAcceso(accessToken)) {
                    return usuarioActivo(jwtTokenProvider.obtenerUsernameDelJwt(accessToken));
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private Usuario usuarioActivo(String email) {
        Usuario usuario = buscarPorEmail(email);
        if (!Boolean.TRUE.equals(usuario.getEstadoActivo())) {
            return null;
        }
        return usuario;
    }

    private Usuario usuarioAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BadCredentialsException("Sesion requerida para esta operacion.");
        }
        return buscarPorEmail(authentication.getName());
    }

    private Usuario buscarPorEmail(String email) {
        return usuarioRepositorio.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado."));
    }

    private boolean consumirCodigoRespaldo(Usuario usuario, String codigo) {
        if (codigo == null || usuario.getTwoFactorBackupCodes() == null
                || usuario.getTwoFactorBackupCodes().isBlank()) {
            return false;
        }
        String limpio = codigo.trim();
        List<String> hashes = new ArrayList<>(Arrays.asList(usuario.getTwoFactorBackupCodes().split(",")));
        for (int i = 0; i < hashes.size(); i++) {
            if (passwordEncoder.matches(limpio, hashes.get(i))) {
                hashes.remove(i);
                usuario.setTwoFactorBackupCodes(hashes.isEmpty() ? null : String.join(",", hashes));
                usuarioRepositorio.save(usuario);
                return true;
            }
        }
        return false;
    }

    private void agregarRefreshCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(Duration.ofMillis(refreshExpirationMs))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void limpiarRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String leerCookie(HttpServletRequest request, String nombre) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (nombre.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String generarTokenAleatorio() {
        return (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "");
    }

    private String hashToken(String token) {
        if (token == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo hashear el token.", ex);
        }
    }

    private void registrarTokenDeDesarrollo(String tipo, String email, String token) {
        if (logDevTokens) {
            log.info("Token de {} para {}: {}", tipo, email, token);
        }
    }
}
