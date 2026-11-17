package com.SaludUnificada.Esu.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoPublicoDtoRequest {

    private LocalDateTime fechaHora;

    // Campos alternativos compatibles con el contrato frontend fecha + hora
    private String fecha;
    private String hora;

    private Long profesionalId;

    private Long centroSaludId;
    private String centroSaludNombre;
    private String consultorioNumero;

    @JsonAlias({"categoria", "tipoTurno"})
    private String tipoAtencion; // "CONSULTA" o "ESTUDIO"

    @JsonAlias({"especialidadNombre", "practica"})
    private String servicio;

    private Long especialidadId;

    @Size(max = 255, message = "El motivo del turno no puede superar los 255 caracteres")
    @JsonAlias({"motivoConsulta"})
    private String motivoTurno;

    @Size(max = 64, message = "La clave de idempotencia no puede superar los 64 caracteres")
    private String idempotencyKey;

    // Datos probatorios de identidad del paciente
    @NotBlank(message = "El DNI del paciente es obligatorio")
    @Pattern(regexp = "^[0-9]{6,10}$", message = "El DNI debe contener entre 6 y 10 dígitos numéricos")
    private String dni;

    @NotBlank(message = "El nombre del paciente es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido del paciente es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    private String apellido;

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    @Size(min = 6, max = 30, message = "El teléfono debe tener entre 6 y 30 caracteres")
    private String telefono;

    @Size(max = 120, message = "El correo electrónico no puede superar los 120 caracteres")
    private String email;

    @Size(max = 100, message = "La cobertura médica no puede superar los 100 caracteres")
    private String obraSocial;

    @Size(max = 50, message = "El número de afiliado no puede superar los 50 caracteres")
    @JsonAlias({"numeroAfiliado"})
    private String nroAfiliado;

    public LocalDateTime obtenerFechaHoraEfectiva() {
        if (this.fechaHora != null) {
            return this.fechaHora;
        }
        if (this.fecha != null && !this.fecha.isBlank() && this.hora != null && !this.hora.isBlank()) {
            LocalDate f = LocalDate.parse(this.fecha.trim());
            String hStr = this.hora.trim();
            if (hStr.length() == 5) {
                hStr += ":00";
            }
            LocalTime h = LocalTime.parse(hStr);
            return f.atTime(h);
        }
        return null;
    }
}
