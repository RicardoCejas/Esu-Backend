package com.SaludUnificada.Esu.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/actuator")
@RequiredArgsConstructor
@Tag(name = "Observabilidad y Salud", description = "Verificación de salud e información operativa del servicio")
public class HealthCheckControlador {

    private final DataSource dataSource;

    @GetMapping("/health")
    @Operation(summary = "Estado de salud del backend y conectividad con la base de datos")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            boolean dbValid = connection.isValid(3);
            if (dbValid) {
                body.put("status", "UP");
                return ResponseEntity.ok(body);
            }
        } catch (Exception ignored) {
            // Do not leak internal database details or stack traces in public health check
        }
        body.put("status", "DOWN");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @GetMapping("/info")
    @Operation(summary = "Información básica pública del servicio")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("application", "ESU");
        body.put("status", "OK");
        body.put("timestamp", OffsetDateTime.now(ZoneId.of("America/Argentina/Cordoba")).toString());
        return ResponseEntity.ok(body);
    }
}
