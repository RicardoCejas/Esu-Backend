package com.SaludUnificada.Esu.evento;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecetaEmitidaEvent {
    private final Long recetaId;
    private final String qrUrl;
}
