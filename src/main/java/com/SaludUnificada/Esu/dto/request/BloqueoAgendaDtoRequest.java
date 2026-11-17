package com.SaludUnificada.Esu.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para registrar un bloqueo preventivo de agenda por licencia, feriado o vacaciones")
public class BloqueoAgendaDtoRequest {

    @Schema(description = "ID del profesional", example = "1")
    private Long profesionalId;

    @Schema(description = "ID del centro de salud opcional", example = "1")
    private Long centroSaludId;
    @NotNull(message = "La fecha y hora de inicio del bloqueo son obligatorias")
    @Schema(description = "Inicio del período de bloqueo (ISO DateTime)", example = "2026-11-01T08:00:00")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha y hora de fin del bloqueo son obligatorias")
    @Schema(description = "Fin del período de bloqueo (ISO DateTime)", example = "2026-11-01T14:00:00")
    private LocalDateTime fechaFin;

    private String horaInicio;
    private String horaFin;

    @NotBlank(message = "El motivo de indisponibilidad es obligatorio")
    @Size(min = 3, max = 255, message = "El motivo debe contener entre 3 y 255 caracteres")
    @Schema(description = "Motivo del bloqueo (Licencia Médica, Congreso, Vacaciones, Feriado)", example = "Licencia Médica por Capacitación")
    private String motivo;

    @JsonSetter("fechaInicio")
    public void parseFechaInicio(String val) {
        if (val == null || val.isBlank()) {
            this.fechaInicio = null;
            return;
        }
        val = val.trim();
        if (val.contains("T")) {
            this.fechaInicio = LocalDateTime.parse(val);
        } else {
            LocalTime lt = LocalTime.MIN;
            if (this.horaInicio != null && !this.horaInicio.isBlank()) {
                try {
                    lt = LocalTime.parse(this.horaInicio.length() == 5 ? this.horaInicio + ":00" : this.horaInicio);
                } catch (Exception ignored) {}
            }
            this.fechaInicio = LocalDate.parse(val).atTime(lt);
        }
    }

    @JsonSetter("fechaFin")
    public void parseFechaFin(String val) {
        if (val == null || val.isBlank()) {
            this.fechaFin = null;
            return;
        }
        val = val.trim();
        if (val.contains("T")) {
            this.fechaFin = LocalDateTime.parse(val);
        } else {
            LocalTime lt = LocalTime.of(23, 59, 59);
            if (this.horaFin != null && !this.horaFin.isBlank()) {
                try {
                    lt = LocalTime.parse(this.horaFin.length() == 5 ? this.horaFin + ":00" : this.horaFin);
                } catch (Exception ignored) {}
            }
            this.fechaFin = LocalDate.parse(val).atTime(lt);
        }
    }

    @JsonSetter("horaInicio")
    public void setHoraInicio(String hora) {
        this.horaInicio = hora;
        if (hora != null && !hora.isBlank() && this.fechaInicio != null) {
            try {
                LocalTime lt = LocalTime.parse(hora.length() == 5 ? hora + ":00" : hora);
                this.fechaInicio = this.fechaInicio.toLocalDate().atTime(lt);
            } catch (Exception ignored) {}
        }
    }

    @JsonSetter("horaFin")
    public void setHoraFin(String hora) {
        this.horaFin = hora;
        if (hora != null && !hora.isBlank() && this.fechaFin != null) {
            try {
                LocalTime lt = LocalTime.parse(hora.length() == 5 ? hora + ":00" : hora);
                this.fechaFin = this.fechaFin.toLocalDate().atTime(lt);
            } catch (Exception ignored) {}
        }
    }
}
