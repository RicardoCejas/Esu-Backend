package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.JwtAuthDtoResponse;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DevAuthController {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/dev-switch-role")
    public ResponseEntity<JwtAuthDtoResponse> devSwitchRole(@RequestBody Map<String, String> body) {
        String nuevoRol = body.get("role");
        if (nuevoRol == null || nuevoRol.isBlank()) {
            throw new IllegalArgumentException("El rol es requerido");
        }
        Usuario dev = usuarioRepositorio.findByEmail("dev@esu.gob.ar")
                .orElseGet(() -> {
                    Usuario nuevoDev = new Usuario();
                    nuevoDev.setEmail("dev@esu.gob.ar");
                    nuevoDev.setPassword(passwordEncoder.encode("DevLocal12345!"));
                    nuevoDev.setRol("ADMIN");
                    nuevoDev.setEstadoActivo(true);
                    nuevoDev.setEmailVerificado(true);
                    nuevoDev.setFechaDeAlta(LocalDate.now());
                    return usuarioRepositorio.save(nuevoDev);
                });

        dev.setRol(nuevoRol.toUpperCase());
        usuarioRepositorio.save(dev);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                dev.getEmail(),
                dev.getPassword(),
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + nuevoRol.toUpperCase()))
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
