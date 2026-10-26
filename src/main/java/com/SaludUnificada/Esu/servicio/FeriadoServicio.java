package com.SaludUnificada.Esu.servicio;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/**
 * Servicio para verificar feriados nacionales y días no laborables oficiales en Argentina.
 * HU-BE-89: No generar cupos en días feriados ni en fechas previamente bloqueadas.
 */
@Service
public class FeriadoServicio {

    private static final Set<MonthDay> FERIADOS_INAMOVIBLES = Set.of(
            MonthDay.of(1, 1),   // Año Nuevo
            MonthDay.of(3, 24),  // Día Nacional de la Memoria por la Verdad y la Justicia
            MonthDay.of(4, 2),   // Día del Veterano y de los Caídos en la Guerra de Malvinas
            MonthDay.of(5, 1),   // Día del Trabajador
            MonthDay.of(5, 25),  // Día de la Revolución de Mayo
            MonthDay.of(6, 20),  // Paso a la Inmortalidad del Gral. Manuel Belgrano
            MonthDay.of(7, 9),   // Día de la Independencia
            MonthDay.of(12, 8),  // Inmaculada Concepción de María
            MonthDay.of(12, 25)  // Navidad
    );

    public boolean esFeriado(LocalDate fecha) {
        if (fecha == null) {
            return false;
        }
        return FERIADOS_INAMOVIBLES.contains(MonthDay.of(fecha.getMonthValue(), fecha.getDayOfMonth()));
    }
}
