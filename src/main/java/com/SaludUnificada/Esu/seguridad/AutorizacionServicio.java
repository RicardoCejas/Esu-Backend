package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Issue #117: autorización a nivel de objeto (anti-IDOR).
 *
 * <p>Centraliza las comprobaciones de propiedad para que los endpoints clínicos no expongan
 * datos de un paciente a otro paciente autenticado.</p>
 */
@Service
public class AutorizacionServicio {

    private final UsuarioRepositorio usuarioRepositorio;

    public AutorizacionServicio(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    public Usuario usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("No hay un usuario autenticado en el contexto.");
        }
        return usuarioRepositorio.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no encontrado."));
    }

    /** {@code true} si el usuario posee rol clínico (PROFESIONAL) o es ADMIN. */
    public boolean esPersonalClinico() {
        return tieneAlgunRol("ROLE_PROFESIONAL", "ROLE_ADMIN");
    }

    public boolean esRecepcionista() {
        return tieneAlgunRol("ROLE_RECEPCIONISTA");
    }

    public boolean puedeAccederAPaciente() {
        return esPersonalClinico() || esRecepcionista();
    }

    private boolean tieneAlgunRol(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            for (String rol : roles) {
                if (rol.equals(authority.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifica que el usuario autenticado pueda acceder a los datos del paciente indicado.
     * El personal clínico y los administradores acceden a cualquier paciente; un PACIENTE
     * sólo a su propia ficha.
     */
    public void verificarAccesoAPaciente(Long pacienteId) {
        if (pacienteId == null) {
            throw new AccessDeniedException("Identificador de paciente inválido.");
        }
        if (puedeAccederAPaciente()) {
            return;
        }
        Usuario usuario = usuarioActual();
        boolean propio = usuario.getPaciente() != null
                && pacienteId.equals(usuario.getPaciente().getId());
        if (!propio) {
            throw new AccessDeniedException("No está autorizado a acceder a datos de otro paciente.");
        }
    }
}
