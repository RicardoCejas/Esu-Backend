package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.entidad.LogAccesoHc;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.LogAccesoHcRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaHistoriaClinicaAspectTest {

    @Mock
    private LogAccesoHcRepositorio logAccesoHcRepositorio;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @InjectMocks
    private AuditoriaHistoriaClinicaAspect aspect;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void medicoConMultiplesPermisosPbac_noFallaPorLongitudRolNiFalsoAdmin() throws Throwable {
        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_MEDICO"),
                new SimpleGrantedAuthority("CONSULTORIOS_ADMINISTRAR"),
                new SimpleGrantedAuthority("PERM_CONSULTORIOS_ADMINISTRAR"),
                new SimpleGrantedAuthority("USUARIOS_ADMINISTRAR"),
                new SimpleGrantedAuthority("PERM_USUARIOS_ADMINISTRAR"),
                new SimpleGrantedAuthority("TURNOS_VER"),
                new SimpleGrantedAuthority("PERM_TURNOS_VER"),
                new SimpleGrantedAuthority("PACIENTES_VER"),
                new SimpleGrantedAuthority("PERM_PACIENTES_VER")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("medico@esu.salud.gob.ar", null, authorities)
        );

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "104.22.72.8, 172.71.100.2, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        Usuario u = new Usuario();
        u.setId(15L);
        when(usuarioRepositorio.findByEmail("medico@esu.salud.gob.ar")).thenReturn(Optional.of(u));

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        when(sig.getName()).thenReturn("obtenerResumenClinico");
        when(joinPoint.getSignature()).thenReturn(sig);
        when(joinPoint.getArgs()).thenReturn(new Object[]{99L});
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.auditarYControlarAcceso(joinPoint);
        assertEquals("ok", result);

        ArgumentCaptor<LogAccesoHc> captor = ArgumentCaptor.forClass(LogAccesoHc.class);
        verify(logAccesoHcRepositorio).save(captor.capture());

        LogAccesoHc saved = captor.getValue();
        assertEquals(15L, saved.getUsuarioId());
        assertEquals(99L, saved.getPacienteId());
        assertEquals("ROLE_MEDICO", saved.getRolUsuario());
        assertTrue(saved.getRolUsuario().length() <= 50);
        assertEquals("104.22.72.8", saved.getIpAddress());
    }

    @Test
    void adminSinRolClinico_esBloqueadoPorSecretoProfesional() {
        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("USUARIOS_ADMINISTRAR")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@esu.gob.ar", null, authorities)
        );

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        assertThrows(AccessDeniedException.class, () -> aspect.auditarYControlarAcceso(joinPoint));
        verifyNoInteractions(logAccesoHcRepositorio);
    }
}
