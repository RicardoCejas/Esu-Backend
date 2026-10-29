package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.JwtAuthDtoResponse;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DevAuthController {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${esu.dev.secret:}")
    private String configuredDevSecret;

    @PostMapping("/dev-switch-role")
    public ResponseEntity<JwtAuthDtoResponse> devSwitchRole(
            @RequestHeader(value = "X-Dev-Secret", required = false) String providedSecret,
            @RequestBody Map<String, String> body) {

        if (configuredDevSecret == null || configuredDevSecret.isBlank() || !configuredDevSecret.equals(providedSecret)) {
            log.warn("Intento no autorizado de uso de /api/auth/dev-switch-role sin clave secreta de desarrollo válida.");
            throw new AccessDeniedException("Acceso no autorizado a herramientas de desarrollo.");
        }

        String nuevoRol = body.get("role");
        if (nuevoRol == null || nuevoRol.isBlank()) {
            throw new IllegalArgumentException("El rol es requerido");
        }

        String rolNormalizado = nuevoRol.toUpperCase().trim();
        List<String> rolesPermitidos = List.of("ADMIN", "MEDICO", "PROFESIONAL", "RECEPCIONISTA", "PACIENTE", "FARMACIA");
        if (!rolesPermitidos.contains(rolNormalizado)) {
            throw new IllegalArgumentException("Rol no reconocido: " + nuevoRol);
        }

        Usuario dev = usuarioRepositorio.findByEmail("dev@esu.gob.ar")
                .orElseGet(() -> {
                    byte[] randomBytes = new byte[24];
                    new SecureRandom().nextBytes(randomBytes);
                    String randomPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

                    Usuario nuevoDev = new Usuario();
                    nuevoDev.setEmail("dev@esu.gob.ar");
                    nuevoDev.setPassword(passwordEncoder.encode(randomPassword));
                    nuevoDev.setRol("ADMIN");
                    nuevoDev.setEstadoActivo(true);
                    nuevoDev.setEmailVerificado(true);
                    nuevoDev.setFechaDeAlta(LocalDate.now());
                    return usuarioRepositorio.save(nuevoDev);
                });

        dev.setRol(rolNormalizado);
        usuarioRepositorio.save(dev);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                dev.getEmail(),
                dev.getPassword(),
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + rolNormalizado))
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = jwtTokenProvider.generarToken(auth);

        JwtAuthDtoResponse response = JwtAuthDtoResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .usuarioId(dev.getId())
                .email(dev.getEmail())
                .rol(dev.getRol())
                .build();

        return ResponseEntity.ok(response);
    }
}
