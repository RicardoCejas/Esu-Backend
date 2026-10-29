package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaBorradorDtoResponse {
    private boolean guardado;
    private long timestamp;
    private String mensaje;
}
