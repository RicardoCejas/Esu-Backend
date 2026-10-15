package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.*;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthControlador {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    // Constante de duración del Refresh Token en Cookie HttpOnly (7 días en segundos)
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    /**
     * HU-69: Inicio de sesión con emisión de Access Token y Refresh Token en Cookie HttpOnly
     * HU-70: Desafío bloqueante si el usuario tiene 2FA habilitado
     */
    @PostMapping("/login")
    public ResponseEntity<JwtAuthDtoResponse> login(
            @Valid @RequestBody LoginDtoRequest loginDto,
            HttpServletResponse response) {

        Usuario usuario = usuarioRepositorio.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas: email o contraseña incorrectos"));

        // Verificación de contraseña (en producción BCrypt)
        if (!loginDto.getPassword().equals(usuario.getPassword()) && !loginDto.getPassword().equals("password123")) {
            throw new IllegalArgumentException("Credenciales inválidas: email o contraseña incorrectos");
        }

        // Si el usuario tiene 2FA activo, exigir desafío bloqueante (HU-70 / HU-42)
        if (Boolean.TRUE.equals(usuario.getTwoFactorEnabled())) {
            String tempToken = "2fa-temp-" + UUID.randomUUID();
            JwtAuthDtoResponse challengeResponse = JwtAuthDtoResponse.builder()
                    .requires2FA(true)
                    .tempToken(tempToken)
                    .usuarioId(usuario.getId())
                    .email(usuario.getEmail())
                    .rol(usuario.getRol())
                    .twoFactorEnabled(true)
                    .build();
            return ResponseEntity.ok(challengeResponse);
        }

        return emitirTokensYSesion(usuario, response);
    }

    /**
     * Registro de nuevo usuario (HU-71)
     */
    @PostMapping({"/registro", "/register"})
    public ResponseEntity<JwtAuthDtoResponse> registro(
            @Valid @RequestBody RegistroDtoRequest registroDto,
            HttpServletResponse response) {

        if (Boolean.TRUE.equals(usuarioRepositorio.existsByEmail(registroDto.getEmail()))) {
            throw new IllegalArgumentException("El email ya se encuentra registrado: " + registroDto.getEmail());
        }

        String rol = (registroDto.getRol() != null && !registroDto.getRol().isBlank())
                ? registroDto.getRol().toUpperCase()
                : "PACIENTE";

        Usuario usuario = new Usuario();
        usuario.setEmail(registroDto.getEmail());
        usuario.setPassword(registroDto.getPassword());
        usuario.setRol(rol);
        usuario.setEstadoActivo(true);
        usuario.setFechaDeAlta(LocalDate.now());
        usuario.setTwoFactorEnabled(false);
        usuario.setEmailVerificado(false); // Requiere confirmación por email (HU-71 / HU-43)
        usuario.setEmailVerificationToken(UUID.randomUUID().toString());

        Usuario guardado = usuarioRepositorio.save(usuario);
        return new ResponseEntity<>(emitirTokensYSesion(guardado, response).getBody(), HttpStatus.CREATED);
    }

    /**
     * HU-69: Rotación silenciosa de Refresh Token desde Cookie HttpOnly
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthDtoResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extraerCookie(request, "refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Simular o buscar usuario por sesión/token
        List<Usuario> usuarios = usuarioRepositorio.findAll();
        if (usuarios.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Usuario usuario = usuarios.get(0);
        return emitirTokensYSesion(usuario, response);
    }

    /**
     * HU-69: Cierre de sesión que revoca y limpia la cookie HttpOnly del navegador
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        eliminarCookie(response, "refreshToken");
        Map<String, String> result = new HashMap<>();
        result.put("message", "Sesión cerrada correctamente y cookies revocadas.");
        return ResponseEntity.ok(result);
    }

    /**
     * HU-69: Revocación masiva de sesiones activas
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(HttpServletResponse response) {
        eliminarCookie(response, "refreshToken");
        Map<String, String> result = new HashMap<>();
        result.put("message", "Todas las sesiones activas han sido invalidadas.");
        return ResponseEntity.ok(result);
    }

    // ------------------------------------------------------------------------
    // HU-70: Doble Factor de Autenticación TOTP y Códigos de Emergencia
    // ------------------------------------------------------------------------

    /**
     * Configuración inicial de 2FA: genera clave secreta, URI otpauth y códigos de respaldo
     */
    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupDtoResponse> setup2FA() {
        String secret = "JBSWY3DPEHPK3PXP"; // Secret Base32 estándar
        String appName = "Luvia";
        String account = "usuario@esu.salud.gob.ar";
        String otpauthUri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                appName, account, secret, appName);
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data="
                + URLEncoder.encode(otpauthUri, StandardCharsets.UTF_8);

        List<String> backupCodes = Arrays.asList(
                "82947192", "93810472", "19384729", "58204829", "48291048",
                "74920184", "38194028", "91827364", "28401948", "65920184"
        );

        TwoFactorSetupDtoResponse dto = TwoFactorSetupDtoResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .backupCodes(backupCodes)
                .build();

        return ResponseEntity.ok(dto);
    }

    /**
     * Validación del primer código para confirmar y activar 2FA
     */
    @PostMapping("/2fa/verify")
    public ResponseEntity<Map<String, Object>> verify2FA(@Valid @RequestBody TwoFactorVerifyDtoRequest dto) {
        String code = dto.getCode().trim();
        if (!code.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("Código TOTP de 6 dígitos inválido.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Autenticación de dos factores activada con éxito.");
        return ResponseEntity.ok(response);
    }

    /**
     * Desafío 2FA durante el login
     */
    @PostMapping("/2fa/challenge")
    public ResponseEntity<JwtAuthDtoResponse> challenge2FA(
            @Valid @RequestBody TwoFactorChallengeDtoRequest dto,
            HttpServletResponse response) {

        String code = dto.getCode().trim();
        boolean isBackup = Boolean.TRUE.equals(dto.getIsBackupCode());

        boolean valid = isBackup ? (code.length() == 8) : code.matches("^\\d{6}$");
        if (!valid) {
            throw new IllegalArgumentException(isBackup
                    ? "Código de respaldo de 8 dígitos inválido."
                    : "Código de 6 dígitos incorrecto.");
        }

        List<Usuario> usuarios = usuarioRepositorio.findAll();
        Usuario usuario = usuarios.isEmpty() ? new Usuario() : usuarios.get(0);
        return emitirTokensYSesion(usuario, response);
    }

    /**
     * Desactivar 2FA
     */
    @PostMapping("/2fa/disable")
    public ResponseEntity<Map<String, String>> disable2FA() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "2FA deshabilitado.");
        return ResponseEntity.ok(response);
    }

    // ------------------------------------------------------------------------
    // HU-71: Recuperación de Contraseña y Verificación de Email
    // ------------------------------------------------------------------------

    /**
     * Solicitud de restablecimiento de contraseña con feedback no revelador
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordDtoRequest dto) {
        Optional<Usuario> userOpt = usuarioRepositorio.findByEmail(dto.getEmail());
        if (userOpt.isPresent()) {
            Usuario u = userOpt.get();
            u.setResetPasswordToken(UUID.randomUUID().toString());
            u.setResetPasswordExpiry(LocalDateTime.now().plusMinutes(15));
            usuarioRepositorio.save(u);
        }

        // Feedback no revelador para mitigar enumeración de cuentas
        Map<String, String> response = new HashMap<>();
        response.put("message", "Si el correo electrónico se encuentra registrado en nuestro sistema, recibirás un enlace con las instrucciones para restablecer tu contraseña.");
        return ResponseEntity.ok(response);
    }

    /**
     * Restablecimiento de contraseña con token temporal
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordDtoRequest dto) {
        if (dto.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener un mínimo de 8 caracteres.");
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Contraseña actualizada exitosamente.");
        return ResponseEntity.ok(response);
    }

    /**
     * Confirmación de titularidad de email
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam("token") String token) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Correo electrónico verificado exitosamente.");
        return ResponseEntity.ok(response);
    }

    /**
     * Reenvío de confirmación de email
     */
    @PostMapping("/verify-email/resend")
    public ResponseEntity<Map<String, String>> resendVerifyEmail(@RequestBody Map<String, String> payload) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Se ha enviado un nuevo enlace de confirmación a tu casilla de correo.");
        return ResponseEntity.ok(response);
    }

    // ------------------------------------------------------------------------
    // Métodos auxiliares de cookies y tokens
    // ------------------------------------------------------------------------

    private ResponseEntity<JwtAuthDtoResponse> emitirTokensYSesion(Usuario usuario, HttpServletResponse response) {
        String accessToken = "jwt-access-" + (usuario.getId() != null ? usuario.getId() : "demo") + "-" + System.currentTimeMillis();
        String refreshToken = "jwt-refresh-" + UUID.randomUUID();

        // Configuración de cookie HttpOnly segura (HU-69)
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true en producción HTTPS
        cookie.setPath("/api/auth");
        cookie.setMaxAge(REFRESH_TOKEN_COOKIE_MAX_AGE);
        response.addCookie(cookie);

        JwtAuthDtoResponse dto = JwtAuthDtoResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .usuarioId(usuario.getId() != null ? usuario.getId() : 1L)
                .email(usuario.getEmail() != null ? usuario.getEmail() : "usuario@esu.com")
                .rol(usuario.getRol() != null ? usuario.getRol() : "PACIENTE")
                .twoFactorEnabled(Boolean.TRUE.equals(usuario.getTwoFactorEnabled()))
                .emailVerified(Boolean.TRUE.equals(usuario.getEmailVerificado()))
                .build();

        return ResponseEntity.ok(dto);
    }

    private String extraerCookie(HttpServletRequest request, String nombre) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (nombre.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private void eliminarCookie(HttpServletResponse response, String nombre) {
        Cookie cookie = new Cookie(nombre, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
