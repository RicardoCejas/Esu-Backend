package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.JwtAuthDtoResponse;
import com.SaludUnificada.Esu.dto.auth.LoginDtoRequest;
import com.SaludUnificada.Esu.dto.auth.RegistroDtoRequest;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import com.SaludUnificada.Esu.seguridad.RevokedTokenService;
import com.SaludUnificada.Esu.seguridad.Roles;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthControlador {

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

    @PostMapping("/login")
    public ResponseEntity<JwtAuthDtoResponse> login(@Valid @RequestBody LoginDtoRequest loginDto) {
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
                .rol(Roles.requerido(usuario.getRol()))
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping({"/registro", "/register"})
    public ResponseEntity<JwtAuthDtoResponse> registro(@Valid @RequestBody RegistroDtoRequest registroDto) {
        if (usuarioRepositorio.existsByEmail(registroDto.getEmail())) {
            throw new IllegalArgumentException("El email ya se encuentra registrado: " + registroDto.getEmail());
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

    /**
     * Issue #117: cierre de sesión. Invalida el token presentado hasta su expiración natural
     * agregando su {@code jti} a la lista de revocados.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                if (jwtTokenProvider.validarToken(token)) {
                    revokedTokenService.revocar(
                            jwtTokenProvider.obtenerJti(token),
                            jwtTokenProvider.obtenerExpiracion(token));
                }
            } catch (Exception ignored) {
                // Token inválido o malformado: el logout es idempotente.
            }
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
