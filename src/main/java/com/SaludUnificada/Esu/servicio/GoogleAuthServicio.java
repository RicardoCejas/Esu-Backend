package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.auth.GoogleIdTokenPayload;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthServicio {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra de forma estrictamente transaccional un nuevo usuario de Google junto a su ficha clínica de Paciente.
     * Si la ficha de paciente falla, la transacción se revierte por completo evitando cuentas huérfanas sin perfil.
     */
    @Transactional(rollbackFor = Exception.class)
    public Usuario registrarUsuarioConPaciente(GoogleIdTokenPayload payload) {
        Usuario usuario = new Usuario();
        usuario.setEmail(payload.getEmail());
        usuario.setGoogleSub(payload.getSub());
        usuario.setAuthProvider("GOOGLE");
        usuario.setRol("PACIENTE");
        usuario.setEstadoActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setFechaDeAlta(LocalDate.now());
        usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        Usuario usuarioGuardado = usuarioRepositorio.save(usuario);

        try {
            Paciente paciente = new Paciente();
            String nombre = payload.getGivenName() != null && !payload.getGivenName().isBlank()
                    ? payload.getGivenName()
                    : (payload.getName() != null ? payload.getName() : "Usuario");
            String apellido = payload.getFamilyName() != null ? payload.getFamilyName() : "";
            paciente.setNombre(nombre.trim());
            paciente.setApellido(apellido.trim());
            String sub = payload.getSub();
            String suffix = (sub != null && sub.length() > 8) ? sub.substring(sub.length() - 8) : String.valueOf(System.currentTimeMillis() % 100000000L);
            paciente.setDni("G-" + suffix);
            paciente.setNroAfiliado("GGL-" + suffix);
            paciente.setTelefono("");
            paciente.setUsuario(usuarioGuardado);

            Paciente pacienteGuardado = pacienteRepositorio.save(paciente);
            usuarioGuardado.setPaciente(pacienteGuardado);
        } catch (Exception e) {
            log.error("Fallo transaccional al crear la ficha de paciente para usuario Google {}: {}", payload.getEmail(), e.getMessage());
            throw new IllegalStateException("No se pudo completar la creación de la ficha médica de paciente para la cuenta de Google.", e);
        }

        return usuarioGuardado;
    }
}
