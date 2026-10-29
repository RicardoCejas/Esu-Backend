package com.SaludUnificada.Esu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class TurnosConfig {
    @Bean
    public ZoneId turnosZoneId(@Value("${esu.turnos.zone:America/Argentina/Cordoba}") String zone) {
        return ZoneId.of(zone);
    }

    @Bean
    public Clock applicationClock(ZoneId turnosZoneId) {
        return Clock.system(turnosZoneId);
    }
}
