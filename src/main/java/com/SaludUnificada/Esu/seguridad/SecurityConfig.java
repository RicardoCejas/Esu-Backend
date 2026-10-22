package com.SaludUnificada.Esu.seguridad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    // Issue #117: orígenes CORS permitidos, configurables y sin comodín "*" con credenciales.
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    // Issue #117: la documentación interactiva puede deshabilitarse fuera de desarrollo.
    @Value("${app.security.swagger-public:true}")
    private boolean swaggerPublico;

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
        configuration.setAllowedOrigins(allowedOrigins);
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
                .authorizeHttpRequests(auth -> {
                        // Rutas públicas de autenticación y documentación
                        auth.requestMatchers("/api/auth/**").permitAll();
                        auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                        if (swaggerPublico) {
                            auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                        }
                        auth.requestMatchers("/api/cie10/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Especialidades y Profesionales: consultas públicas para catálogo
                        .requestMatchers(HttpMethod.GET, "/api/especialidades/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/especialidades/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/especialidades/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/profesionales/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/profesionales/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/profesionales/**").hasRole("ADMIN")

                        // Gestión de Usuarios y Auditoría / Métricas: restringido a Administradores
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Obras Sociales y Centros de Salud: GET público/autenticado, modificación ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/obras-sociales/**").permitAll()
                        .requestMatchers("/api/obras-sociales/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/centros-salud/**").permitAll()
                        .requestMatchers("/api/centros-salud/**").hasRole("ADMIN")

                        .requestMatchers("/api/medicos/asignar-consultorio").hasAnyRole("PROFESIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/medicos/**").permitAll()

                        // Pacientes: recepción rápida permitida a ADMIN y personal, resto de operaciones
                        .requestMatchers(HttpMethod.POST, "/api/pacientes/recepcion").hasAnyRole("ADMIN", "PROFESIONAL", "PACIENTE", "RECEPCIONISTA")
                        .requestMatchers("/api/pacientes/**").hasAnyRole("PACIENTE", "PROFESIONAL", "ADMIN", "RECEPCIONISTA")

                        // Turnos, Historias Clínicas, Recetas y Evoluciones: roles autenticados
                        .requestMatchers("/api/turnos/**").hasAnyRole("PACIENTE", "PROFESIONAL", "ADMIN", "RECEPCIONISTA")
                        .requestMatchers("/api/historias-clinicas/**").hasAnyRole("PROFESIONAL", "ADMIN", "PACIENTE")
                        .requestMatchers("/api/recetas/**").hasAnyRole("PACIENTE", "PROFESIONAL", "ADMIN")
                        .requestMatchers("/api/evoluciones/**").hasAnyRole("PROFESIONAL", "ADMIN")

                        .anyRequest().authenticated();
                });

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
