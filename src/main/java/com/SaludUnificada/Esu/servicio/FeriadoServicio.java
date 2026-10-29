package com.SaludUnificada.Esu.servicio;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

@Service
public class FeriadoServicio {

    private static final Set<MonthDay> FERIADOS_INAMOVIBLES = Set.of(
            MonthDay.of(1, 1),
            MonthDay.of(3, 24),
            MonthDay.of(4, 2),
            MonthDay.of(5, 1),
            MonthDay.of(5, 25),
            MonthDay.of(6, 20),
            MonthDay.of(7, 9),
            MonthDay.of(12, 8),
            MonthDay.of(12, 25)
    );

    public boolean esFeriado(LocalDate fecha) {
        if (fecha == null) {
            return false;
        }
        return FERIADOS_INAMOVIBLES.contains(MonthDay.of(fecha.getMonthValue(), fecha.getDayOfMonth()));
    }
    public boolean esDiaHabil(LocalDate fecha) {
        if (fecha == null) return false;
        java.time.DayOfWeek day = fecha.getDayOfWeek();
        if (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY) {
            return false;
        }
        return !esFeriado(fecha);
    }
}
