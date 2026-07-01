package com.ventas.key.mis.productos.models.abonos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransferirAbonoRequest {
    @NotNull(message = "La variante destino es obligatoria")
    private Integer nuevaVarianteId;

    @NotNull @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    @NotNull @Positive(message = "El precio debe ser mayor a 0")
    private Double precioUnitario;

    @NotNull(message = "El usuarioId es obligatorio")
    private Integer usuarioId;
}
