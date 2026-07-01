package com.ventas.key.mis.productos.models.abonos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AbonoRequest {

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private Double monto;

    private LocalDate fechaPago;

    // EFECTIVO | TRANSFERENCIA  (TARJETA no aplica en crédito — tiene comisión)
    private String metodoPago = "EFECTIVO";

    // Para TRANSFERENCIA: número de referencia
    private String nota;

    // Solo EFECTIVO: monto entregado por el cliente para calcular cambio
    private Double montoDado;

    @NotNull(message = "El usuarioId es obligatorio")
    private Integer usuarioId;
}
