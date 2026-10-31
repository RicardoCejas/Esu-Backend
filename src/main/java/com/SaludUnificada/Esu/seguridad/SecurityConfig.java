package com.SaludUnificada.Esu.seguridad;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,https://cruzdeleje.gob.ar}")
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
        List<String> origins = Arrays.stream(allowedOriginsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "X-Dev-Secret"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
                        auth.requestMatchers("/api/auth/login", "/api/auth/registro", "/api/auth/register").permitAll();
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

                        auth.requestMatchers(HttpMethod.PATCH, "/api/usuarios/preferencias").authenticated();
                        auth.requestMatchers("/api/usuarios/**").hasRole("ADMIN");

                        auth.requestMatchers(HttpMethod.GET, "/api/centros-salud/**").permitAll();
                        auth.requestMatchers("/api/centros-salud/**").hasRole("ADMIN");

                        auth.requestMatchers("/api/medicos/asignar-consultorio").hasAnyRole("PROFESIONAL", "ADMIN");
                        auth.requestMatchers(HttpMethod.GET, "/api/medicos/**").permitAll();

                        auth.requestMatchers(HttpMethod.POST, "/api/pacientes/recepcion").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers(HttpMethod.GET, "/api/pacientes", "/api/pacientes/obtener/todos", "/api/pacientes/paginado").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers(HttpMethod.DELETE, "/api/pacientes/**").hasRole("ADMIN");
                        auth.requestMatchers("/api/pacientes/fusionar").hasRole("ADMIN");
                        auth.requestMatchers("/api/pacientes/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN", "RECEPCIONISTA");

                        auth.requestMatchers(HttpMethod.DELETE, "/api/turnos/**").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers("/api/turnos/obtener/todos").hasAnyRole("ADMIN", "RECEPCIONISTA");
                        auth.requestMatchers("/api/turnos/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "RECEPCIONISTA", "ADMIN");

                        auth.requestMatchers("/api/historias-clinicas/obtener/todos").hasRole("ADMIN");
                        auth.requestMatchers(HttpMethod.DELETE, "/api/historias-clinicas/**").hasRole("ADMIN");
                        auth.requestMatchers("/api/historias-clinicas/**").hasAnyRole("PROFESIONAL", "MEDICO", "ADMIN", "PACIENTE");

                        auth.requestMatchers("/api/recetas/dispensar").hasAnyRole("FARMACIA", "ADMIN");
                        auth.requestMatchers("/api/recetas/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN", "FARMACIA");
                        auth.requestMatchers("/api/evoluciones/**").hasAnyRole("PROFESIONAL", "MEDICO", "ADMIN");

                        auth.requestMatchers("/api/estudios/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN");
                        auth.requestMatchers("/api/consentimientos/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN");

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
