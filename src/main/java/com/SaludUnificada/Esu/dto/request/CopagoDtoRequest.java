package com.SaludUnificada.Esu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de registro de cobro de copago o bono de consulta")
public class CopagoDtoRequest {

    @NotNull(message = "El monto del copago es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El monto del copago no puede ser negativo")
    @Schema(description = "Monto abonado en concepto de copago o coseguro", example = "1500.00")
    private BigDecimal monto;

    @NotBlank(message = "El número de bono o comprobante es obligatorio")
    @Schema(description = "Número de bono o comprobante emitido por la obra social o prepaga", example = "BONO-998877")
    private String numeroBono;

    @NotBlank(message = "El método de pago es obligatorio (EFECTIVO, TARJETA, TRANSFERENCIA)")
    @Schema(description = "Método de pago utilizado", example = "EFECTIVO")
    private String metodoPago;
}
