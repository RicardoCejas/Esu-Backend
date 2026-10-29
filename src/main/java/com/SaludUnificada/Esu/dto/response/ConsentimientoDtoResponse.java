package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsentimientoDtoResponse {
    private Long id;
    private Long pacienteId;
    private Long profesionalId;
    private String profesionalNombre;
    private String profesionalMatricula;
    private String procedimiento;
    private String textoLegal;
    private String firmaDigitalBase64;
    private String ipFirma;
    private String hashIntegridadSha256;
    private LocalDateTime fechaHoraFirma;
    private String estado;
    private String motivoRevocacion;
    private LocalDateTime fechaHoraRevocacion;
}
