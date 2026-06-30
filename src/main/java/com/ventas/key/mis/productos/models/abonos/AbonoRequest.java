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

    // EFECTIVO | TRANSFERENCIA | TARJETA
    private String metodoPago = "EFECTIVO";

    // Para TRANSFERENCIA usar este campo para el número de referencia
    private String nota;

    @NotNull(message = "El usuarioId es obligatorio")
    private Integer usuarioId;
}
