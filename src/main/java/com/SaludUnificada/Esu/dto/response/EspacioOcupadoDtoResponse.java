package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EspacioOcupadoDtoResponse {
    private Long pacienteId;
    private int totalEstudios;
    private long totalBytes;
    private double totalMegabytes;
}
