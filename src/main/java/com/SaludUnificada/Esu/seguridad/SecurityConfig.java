package com.SaludUnificada.Esu.seguridad;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.CorsFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsConfig;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> patterns = new ArrayList<>();
        patterns.add("https://esuluvia.vercel.app");
        patterns.add("https://*.vercel.app");
        patterns.add("https://*.onrender.com");
        patterns.add("http://localhost:[*]");
        patterns.add("http://127.0.0.1:[*]");
        patterns.add("http://localhost");
        patterns.add("http://127.0.0.1");

        if (allowedOriginsConfig != null && !allowedOriginsConfig.isBlank()) {
            Arrays.stream(allowedOriginsConfig.split(","))
                    .map(String::trim)
                    .map(s -> s.replaceAll("/+$", ""))
                    .filter(s -> !s.isEmpty())
                    .forEach(o -> {
                        if (!patterns.contains(o)) {
                            patterns.add(o);
                        }
                    });
        }

        configuration.setAllowedOriginPatterns(patterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "X-Dev-Secret", "X-XSRF-TOKEN", "x-xsrf-token", "X-CSRF-TOKEN", "Idempotency-Key", "X-Idempotency-Key", "*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean isDevOrLocal = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("local"));

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none';"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(permissions -> permissions.policy("geolocation=(), microphone=(), camera=()"))
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                        auth.requestMatchers("/api/auth/login", "/api/auth/registro", "/api/auth/register", "/api/auth/google").permitAll();
                        auth.requestMatchers("/api/auth/refresh", "/api/auth/logout").permitAll();
                        auth.requestMatchers("/api/auth/solicitar-recuperacion", "/api/auth/forgot-password").permitAll();
                        auth.requestMatchers("/api/auth/resetear-password", "/api/auth/reset-password").permitAll();
                        auth.requestMatchers("/api/auth/verify-email", "/api/auth/verify-email/resend").permitAll();
                        auth.requestMatchers("/api/auth/validar-token").permitAll();

                        if (isDevOrLocal) {
                            auth.requestMatchers("/api/auth/dev-switch-role").permitAll();
                            auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                        } else {
                            auth.requestMatchers("/api/auth/dev-switch-role").denyAll();
                            auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN");
                        }

                        auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                        auth.requestMatchers("/actuator/**").hasRole("ADMIN");
                        auth.requestMatchers("/api/cie10/**").permitAll();
                        auth.requestMatchers("/api/public/**").permitAll();
                        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                        auth.requestMatchers(HttpMethod.GET, "/api/especialidades/**").permitAll();
                        auth.requestMatchers(HttpMethod.POST, "/api/especialidades/**").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers(HttpMethod.PUT, "/api/especialidades/**").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers(HttpMethod.DELETE, "/api/especialidades/**").hasAnyRole("ADMIN", "RECEPCIONISTA");

                        auth.requestMatchers(HttpMethod.GET, "/api/profesionales/**").permitAll();
                        auth.requestMatchers(HttpMethod.POST, "/api/profesionales/**").hasAnyRole("ADMIN", "PROFESIONAL", "MEDICO", "RECEPCIONISTA");
                        auth.requestMatchers(HttpMethod.PUT, "/api/profesionales/**").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers(HttpMethod.DELETE, "/api/profesionales/**").hasAnyRole("ADMIN", "RECEPCIONISTA");

                        auth.requestMatchers(HttpMethod.GET, "/api/disponibilidades/**").permitAll();
                        auth.requestMatchers("/api/disponibilidades/**").hasAnyRole("MEDICO", "PROFESIONAL", "ADMIN");
                        auth.requestMatchers("/api/bloqueos-agenda/**").hasAnyRole("ADMIN", "PROFESIONAL", "MEDICO", "RECEPCIONISTA");

                        auth.requestMatchers(HttpMethod.PATCH, "/api/usuarios/preferencias").authenticated();
                        auth.requestMatchers("/api/usuarios/me/**").authenticated();
                        auth.requestMatchers(HttpMethod.GET, "/api/usuarios", "/api/usuarios/obtener/todos", "/api/usuarios/obtener/*").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers(HttpMethod.POST, "/api/usuarios", "/api/usuarios/crear").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers(HttpMethod.PATCH, "/api/usuarios/*/estado").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers(HttpMethod.PUT, "/api/usuarios/*/asignacion").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers(HttpMethod.DELETE, "/api/usuarios/eliminar/*").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers("/api/usuarios/**").hasRole("ADMIN");
                        auth.requestMatchers("/api/permisos/**").hasAnyRole("ADMIN", "RECEPCIONISTA");

                        auth.requestMatchers(HttpMethod.GET, "/api/centros-salud/**").permitAll();
                        auth.requestMatchers("/api/centros-salud/**").hasAnyRole("ADMIN", "RECEPCIONISTA");

                        auth.requestMatchers("/api/medicos/asignar-consultorio").hasAnyRole("PROFESIONAL", "ADMIN");
                        auth.requestMatchers(HttpMethod.GET, "/api/medicos/**").permitAll();

                        auth.requestMatchers(HttpMethod.POST, "/api/pacientes", "/api/pacientes/crear", "/api/pacientes/recepcion").hasAnyRole("ADMIN", "RECEPCIONISTA", "MEDICO", "PROFESIONAL");
                        auth.requestMatchers(HttpMethod.GET, "/api/pacientes", "/api/pacientes/obtener/todos", "/api/pacientes/paginado").hasAnyRole("ADMIN", "RECEPCIONISTA", "MEDICO", "PROFESIONAL");
                        auth.requestMatchers(HttpMethod.DELETE, "/api/pacientes/**").hasRole("ADMIN");
                        auth.requestMatchers("/api/pacientes/fusionar").hasRole("ADMIN");
                        auth.requestMatchers("/api/pacientes/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN", "RECEPCIONISTA");

                        auth.requestMatchers(HttpMethod.GET, "/api/turnos/disponibles").permitAll();
                        auth.requestMatchers(HttpMethod.POST, "/api/turnos/publico/reservar").permitAll();
                        auth.requestMatchers(HttpMethod.GET, "/api/turnos/publico/cartelera/**").permitAll();
                        auth.requestMatchers(HttpMethod.DELETE, "/api/turnos/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers("/api/turnos/obtener/todos").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers("/api/turnos/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "RECEPCIONISTA", "ADMIN");

                        auth.requestMatchers("/api/historias-clinicas/obtener/todos").denyAll();
                        auth.requestMatchers(HttpMethod.DELETE, "/api/historias-clinicas/**").denyAll();
                        auth.requestMatchers("/api/historias-clinicas/**").hasAnyRole("PROFESIONAL", "MEDICO", "PACIENTE");

                        auth.requestMatchers("/api/recetas/dispensar").hasAnyRole("FARMACIA", "ADMIN", "MEDICO", "PROFESIONAL", "RECEPCIONISTA");
                        auth.requestMatchers("/api/recetas/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "FARMACIA", "ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers("/api/evoluciones/**").hasAnyRole("PROFESIONAL", "MEDICO");

                        auth.requestMatchers("/api/estudios/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO");
                        auth.requestMatchers("/api/consentimientos/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO");

                        auth.requestMatchers("/api/documentos/verificar/**", "/api/documentos/verificar-token/**").permitAll();
                        auth.requestMatchers("/api/documentos/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN");

                        auth.requestMatchers(HttpMethod.GET, "/api/plantillas-evolucion/**").permitAll();
                        auth.requestMatchers(HttpMethod.POST, "/api/plantillas-evolucion/**").hasAnyRole("PROFESIONAL", "MEDICO", "ADMIN");

                        auth.requestMatchers("/api/notificaciones/**").authenticated();

                        auth.anyRequest().authenticated();
                });

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
