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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
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
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas de autenticación y documentación
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/cie10/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Especialidades y Profesionales: consultas públicas para catálogo
                        .requestMatchers(HttpMethod.GET, "/api/especialidades/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/especialidades/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/especialidades/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/profesionales/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/profesionales/**").hasAnyRole("ADMIN", "PROFESIONAL", "MEDICO")
                        .requestMatchers(HttpMethod.DELETE, "/api/profesionales/**").hasRole("ADMIN")

                        // Grillas Horarias y Disponibilidad (HU-BE-89)
                        .requestMatchers(HttpMethod.GET, "/api/disponibilidades/**").permitAll()
                        .requestMatchers("/api/disponibilidades/**").hasAnyRole("MEDICO", "PROFESIONAL", "ADMIN")

                        // Gestión de Usuarios y Auditoría / Métricas: restringido a Administradores
                        .requestMatchers(HttpMethod.PATCH, "/api/usuarios/preferencias").authenticated()
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/metricas/**").hasRole("ADMIN")
                        .requestMatchers("/api/seguridad/**").hasRole("ADMIN")

                        // Obras Sociales y Centros de Salud: GET público/autenticado, modificación ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/obras-sociales/**").permitAll()
                        .requestMatchers("/api/obras-sociales/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/centros-salud/**").permitAll()
                        .requestMatchers("/api/centros-salud/**").hasRole("ADMIN")

                        .requestMatchers("/api/medicos/asignar-consultorio").hasAnyRole("PROFESIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/medicos/**").permitAll()

                        // Pacientes: recepción rápida permitida a ADMIN y personal, resto de operaciones
                        .requestMatchers(HttpMethod.POST, "/api/pacientes/recepcion").hasAnyRole("ADMIN", "PROFESIONAL", "PACIENTE")
                        .requestMatchers("/api/pacientes/**").hasAnyRole("PACIENTE", "PROFESIONAL", "ADMIN")

                        // Turnos, Historias Clínicas, Recetas y Evoluciones: roles autenticados
                        .requestMatchers("/api/turnos/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "RECEPCIONISTA", "ADMIN")
                        .requestMatchers("/api/historias-clinicas/**").hasAnyRole("PROFESIONAL", "MEDICO", "ADMIN", "PACIENTE")
                        .requestMatchers("/api/recetas/**").hasAnyRole("PACIENTE", "PROFESIONAL", "MEDICO", "ADMIN")
                        .requestMatchers("/api/evoluciones/**").hasAnyRole("PROFESIONAL", "MEDICO", "ADMIN")

                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
