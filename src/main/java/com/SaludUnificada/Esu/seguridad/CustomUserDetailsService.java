package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepositorio.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        // Issue #119: un rol ausente o desconocido no se autentica con permisos por defecto.
        String rol = Roles.normalizar(usuario.getRol());
        if (rol == null) {
            throw new UsernameNotFoundException(
                    "El usuario " + email + " tiene un rol no reconocido.");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + rol));

        return new User(
                usuario.getEmail(),
                usuario.getPassword(),
                Boolean.TRUE.equals(usuario.getEstadoActivo()),
                true,
                true,
                true,
                authorities
        );
    }
}
