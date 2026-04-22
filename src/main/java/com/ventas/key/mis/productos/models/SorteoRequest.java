package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SorteoRequest {

    @Min(value = 1, message = "La vuelta actual debe ser al menos 1")
    private int vueltaActual;

    @Min(value = 1, message = "El total de vueltas debe ser al menos 1")
    private int totalVueltas;
}