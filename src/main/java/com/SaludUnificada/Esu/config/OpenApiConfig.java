package com.SaludUnificada.Esu.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ESU - Sistema Integral de Gestión de Turnos y Casos")
                        .description("API REST oficial para la red unificada de efectores sanitarios de Cruz del Eje, Córdoba.\n\n" +
                                "Permite la gestión de pacientes, profesionales médicos, centros de salud y consultorios físicos, " +
                                "historias clínicas digitales, evolución médica, recetas electrónicas con código QR, asignación de turnos y sobreturnos, " +
                                "y auditoría de accesos clínicos.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Equipo de Ingeniería ESU")
                                .email("soporte@esu.salud.gob.ar"))
                        .license(new License()
                                .name("Uso Institucional Oficial - Ministerio de Salud")
                                .url("https://salud.cba.gov.ar")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese el token JWT obtenido desde `/api/auth/login` con prefijo Bearer.")));
    }
}
