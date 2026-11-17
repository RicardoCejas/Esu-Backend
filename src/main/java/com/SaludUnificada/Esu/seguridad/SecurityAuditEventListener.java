package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.servicio.AuditoriaSeguridadServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditEventListener {

    @Autowired
    private AuditoriaSeguridadServicio auditoriaSeguridadServicio;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = event.getAuthentication().getName();
        }
        auditoriaSeguridadServicio.registrarLoginExitoso(username);
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        String errorMsg = event.getException() != null ? event.getException().getMessage() : "Credenciales inválidas";
        auditoriaSeguridadServicio.registrarLoginFallido(username, errorMsg);
    }
}
