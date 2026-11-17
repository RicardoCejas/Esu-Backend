package com.SaludUnificada.Esu.servicio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecetaVencimientoScheduler {

    private final RecetaDigitalServicio recetaDigitalServicio;

    @Scheduled(cron = "${esu.recetas.vencimiento.cron:0 0 * * * *}")
    public void expirarRecetasVencidas() {
        try {
            int vencidas = recetaDigitalServicio.marcarRecetasVencidasBatch(100);
            if (vencidas > 0) {
                log.info("Scheduler de vencimiento: se marcaron {} recetas como VENCIDA.", vencidas);
            }
        } catch (Exception e) {
            log.error("Error al ejecutar scheduler de expiración de recetas: {}", e.getMessage());
        }
    }
}
