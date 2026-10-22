package com.SaludUnificada.Esu.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TurnoMostradorDtoRequest {
    private String pacienteId;
    private String profesionalId;
    private String centroSaludId;
    private String fecha;
    private String hora;
    private String fechaHora;
    private String motivoConsulta;
    private String motivoTurno;
    private String estado;
    private String obraSocial;
    private String numeroAfiliado;
    private BigDecimal copagoMonto;
    private String numeroBono;
    private Boolean esPrimeraVez;
    private String consultorioNumero;
    private String planta;
}
