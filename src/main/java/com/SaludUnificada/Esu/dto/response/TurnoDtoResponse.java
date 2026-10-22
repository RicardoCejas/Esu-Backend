package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TurnoDtoResponse {
    private Long id;
    private LocalDateTime fechaHora;
    private String fecha;
    private String hora;
    private String estado;
    private String motivoTurno;
    private String motivoConsulta;
    private String codigoVerificacion;
    private String obraSocial;
    private String numeroAfiliado;
    private BigDecimal copagoMonto;
    private String numeroBono;
    private Boolean esPrimeraVez;
    private String consultorioNumero;
    private String planta;
    private String justificativoCancelacion;
    private String horaCheckIn;
    private String horaLlamado;
    private LocalDateTime fechaCreacion;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteDni;
    private String pacienteTelefono;
    private Long profesionalId;
    private String profesionalNombre;
    private String especialidadNombre;
    private Long centroSaludId;
    private String centroSaludNombre;
    private PacienteDtoResponse paciente;
    private ProfesionalDtoResponse profesional;
}
