package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RamoArmadoAccesorioResponseDto {
    private Integer accesorioId;
    private String nombre;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
    private Integer varianteId;
}
