package com.SaludUnificada.Esu.seguridad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
        configuration.setAllowedOriginPatterns(List.of("http://localhost:[*]", "http://127.0.0.1:[*]", "https://*.cruzdeleje.gob.ar"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/cie10/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/especialidades/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/especialidades/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/especialidades/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/profesionales/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/profesionales/**").hasAnyRole("ADMIN", "PROFESIONAL", "MEDICO")
                        .requestMatchers(HttpMethod.DELETE, "/api/profesionales/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/disponibilidades/**").permitAll()
                        .requestMatchers("/api/disponibilidades/**").hasAnyRole("MEDICO", "PROFESIONAL", "ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/api/usuarios/preferencias").authenticated()
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/reportes/**").hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers("/api/admin/metricas/**").hasAnyRole("ADMIN", "MEDICO", "PROFESIONAL")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/metricas/**").hasRole("ADMIN")
                        .requestMatchers("/api/seguridad/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/obras-sociales/**").permitAll()
                        .requestMatchers("/api/obras-sociales/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/centros-salud/**").permitAll()
                        .requestMatchers("/api/centros-salud/**").hasRole("ADMIN")

                        .requestMatchers("/api/medicos/asignar-consultorio").hasAnyRole("PROFESIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/medicos/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/pacientes/recepcion").hasAnyRole("ADMIN", "PROFESIONAL", "PACIENTE")
                        .requestMatchers("/api/pacientes/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN", "RECEPCIONISTA")

                        .requestMatchers(HttpMethod.DELETE, "/api/turnos/**").hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers("/api/turnos/obtener/todos").hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers("/api/turnos/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "RECEPCIONISTA", "ADMIN")
                        .requestMatchers("/api/historias-clinicas/obtener/todos").hasAnyRole("PROFESIONAL", "MEDICO", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/historias-clinicas/**").hasRole("ADMIN")
                        .requestMatchers("/api/historias-clinicas/**").hasAnyRole("PROFESIONAL", "MEDICO", "ADMIN", "PACIENTE")

                        .requestMatchers("/api/recetas/dispensar").permitAll()
                        .requestMatchers("/api/recetas/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN")
                        .requestMatchers("/api/evoluciones/**").hasAnyRole("PROFESIONAL", "MEDICO", "ADMIN")

                        .requestMatchers("/api/estudios/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN")
                        .requestMatchers("/api/consentimientos/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN")

                        .requestMatchers("/api/documentos/verificar/**").permitAll()
                        .requestMatchers("/api/documentos/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/plantillas-evolucion/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/plantillas-evolucion/**").hasAnyRole("PROFESIONAL", "MEDICO", "ADMIN")

                        .requestMatchers("/api/notificaciones/**").authenticated()

                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
