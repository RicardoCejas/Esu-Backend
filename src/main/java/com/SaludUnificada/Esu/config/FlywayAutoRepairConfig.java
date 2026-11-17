package com.SaludUnificada.Esu.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Estrategia de migración Flyway resiliente para despliegues productivos.
 * Ejecuta un 'repair' preventivo antes de 'migrate' para depurar automáticamente
 * migraciones interrumpidas o fallidas y realinear checksums sin bloquear el arranque del contenedor.
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", matchIfMissing = true)
public class FlywayAutoRepairConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayAutoRepairConfig.class);

    @Bean
    public FlywayMigrationStrategy repairAndMigrateStrategy() {
        return flyway -> {
            try {
                log.info("Ejecutando Flyway repair preventivo para sanear historial de migraciones...");
                flyway.repair();
            } catch (Exception ex) {
                log.warn("Aviso en Flyway repair: {}", ex.getMessage());
            }
            flyway.migrate();
        };
    }
}
