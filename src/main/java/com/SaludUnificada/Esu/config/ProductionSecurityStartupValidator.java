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

    @PostConstruct
    public void validateConfiguration() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isProd = activeProfiles.contains("prod") || activeProfiles.contains("production");
        boolean isDevOrLocal = activeProfiles.contains("dev") || activeProfiles.contains("local");

        if (isProd && isDevOrLocal) {
            throw new IllegalStateException("FATAL: Conflicto crítico de perfiles de Spring Boot. El perfil productivo ('prod') no puede ejecutarse en combinación con perfiles de desarrollo ('dev' o 'local').");
        }

        if (isProd) {
            log.info("Validando configuración de seguridad obligatoria para entorno PRODUCCIÓN...");

            // 1. Validar JWT Secret
            if (jwtSecret == null || jwtSecret.trim().isEmpty() || jwtSecret.length() < 32) {
                throw new IllegalStateException("FATAL: En entorno de producción se requiere definir 'APP_JWT_SECRET' con una clave de al menos 32 caracteres (256 bits).");
            }
            if (jwtSecret.contains("MiClaveSecretaSuperSegura") || jwtSecret.contains("secret") || jwtSecret.contains("123456")) {
                throw new IllegalStateException("FATAL: 'APP_JWT_SECRET' no puede contener valores predeterminados o predecibles en producción.");
            }

            // 2. Validar que la base de datos no use usuario 'root' ni contraseña vacía
            if ("root".equalsIgnoreCase(datasourceUsername)) {
                throw new IllegalStateException("FATAL: Violación de mínimos privilegios: La aplicación no puede conectarse a MySQL como usuario 'root' en producción.");
            }
            if (datasourcePassword == null || datasourcePassword.trim().isEmpty() || "root".equalsIgnoreCase(datasourcePassword)) {
                throw new IllegalStateException("FATAL: Contraseña de base de datos MySQL vacía o insegura para producción.");
            }

            // 3. Validar CORS sin comodines
            if (allowedOrigins == null || allowedOrigins.contains("*")) {
                throw new IllegalStateException("FATAL: Configuración CORS permisiva detectada en producción. No se permite comodín ('*').");
            }

            log.info("Verificación de configuración productiva completada satisfactoriamente.");
        }
    }
}
