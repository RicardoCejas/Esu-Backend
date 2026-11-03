package com.SaludUnificada.Esu.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionSecurityStartupValidator {

    private final Environment environment;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled = false;

    @Value("${app.auth.google.enabled:false}")
    private boolean googleAuthEnabled = false;

    @Value("${google.client.id:}")
    private String googleClientId;

    @Value("${spring.flyway.enabled:true}")
    private boolean flywayEnabled = true;

    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String hibernateDdlAuto = "validate";

    @PostConstruct
    public void validateConfiguration() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isProd = activeProfiles.contains("prod") || activeProfiles.contains("production");
        boolean isDevOrLocal = activeProfiles.contains("dev") || activeProfiles.contains("local");

        if (isProd && isDevOrLocal) {
            log.warn("ADVERTENCIA: Conflicto potencial de perfiles Spring Boot (prod activo junto a dev/local).");
        }

        if (isProd) {
            log.info("Validando configuración de seguridad para entorno PRODUCCIÓN...");

            // 0. Validar Flyway y Hibernate DDL
            if (!flywayEnabled) {
                log.warn("ADVERTENCIA: 'spring.flyway.enabled' está configurado en false.");
            }
            if (!"validate".equalsIgnoreCase(hibernateDdlAuto)) {
                log.warn("ADVERTENCIA: 'spring.jpa.hibernate.ddl-auto' tiene el valor '{}' en vez de 'validate'.", hibernateDdlAuto);
            }

            // 1. Validar JWT Secret
            if (jwtSecret == null || jwtSecret.trim().isEmpty() || jwtSecret.length() < 32) {
                log.warn("ADVERTENCIA: 'APP_JWT_SECRET' ausente o de longitud menor a 32 caracteres. Se usará el valor configurado por defecto.");
            }

            // 2. Validar credenciales de base de datos
            if ("root".equalsIgnoreCase(datasourceUsername)) {
                log.warn("AVISO: Conexión de base de datos MySQL como usuario 'root'.");
            }
            if (datasourcePassword == null || datasourcePassword.trim().isEmpty()) {
                log.warn("AVISO: Contraseña de base de datos MySQL no provista o vacía.");
            }

            // 3. Validar CORS
            if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
                log.warn("AVISO: 'app.cors.allowed-origins' no especificado explícitamente. Se aplicarán orígenes seguros por defecto.");
            }

            // 4. Validar Frontend URL público
            if (frontendUrl == null || frontendUrl.trim().isEmpty()) {
                log.warn("AVISO: 'FRONTEND_URL' no provisto. Se aplicará la URL pública de producción por defecto.");
            }

            // 5. Validar host de correo electrónico si está habilitado
            if (mailEnabled) {
                if (mailHost == null || mailHost.trim().isEmpty() || "localhost".equalsIgnoreCase(mailHost)) {
                    log.warn("ADVERTENCIA: 'spring.mail.host' no configurado para un servidor SMTP externo.");
                }
            } else {
                log.info("Envío de correos SMTP desactivado para entorno de demostración (app.mail.enabled=false).");
            }

            // 6. Validar Google Client ID si está habilitado
            if (googleAuthEnabled) {
                if (googleClientId == null || googleClientId.trim().isEmpty()) {
                    log.warn("ADVERTENCIA: Google Auth habilitado pero 'GOOGLE_CLIENT_ID' no configurado.");
                }
            }

            log.info("Verificación de configuración productiva completada satisfactoriamente (modo tolerante a fallos de arranque).");
        }
    }
}
