package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final UsuarioRepositorio usuarioRepositorio;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = obtenerJwtDeSolicitud(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validarToken(token)) {
            try {
                String username = jwtTokenProvider.obtenerUsernameDelJwt(token);
                Integer tokenVersionEnJwt = jwtTokenProvider.obtenerTokenVersionDelJwt(token);

                Optional<Usuario> usuarioOpt = usuarioRepositorio.findByEmail(username);
                if (usuarioOpt.isPresent()) {
                    Usuario usuario = usuarioOpt.get();

                    // Check if account was deactivated or revoked
                    if (!Boolean.TRUE.equals(usuario.getEstadoActivo())) {
                        log.warn("Rechazo de token JWT: la cuenta de usuario {} está inactiva.", username);
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Check if token version was revoked (due to password change, role update, etc.)
                    if (tokenVersionEnJwt != null && usuario.getTokenVersion() != null &&
                            !tokenVersionEnJwt.equals(usuario.getTokenVersion())) {
                        log.warn("Rechazo de token JWT revocado por cambio de credenciales para {}", username);
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (Exception ex) {
                log.debug("Fallo al autenticar contexto con JWT: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String obtenerJwtDeSolicitud(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
