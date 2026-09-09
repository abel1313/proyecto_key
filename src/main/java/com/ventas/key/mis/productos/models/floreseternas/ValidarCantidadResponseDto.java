package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidarCantidadResponseDto {
    private Integer cantidadSolicitada;
    private Double precioCantidadSolicitada;
    private Boolean valida;
    private String mensaje;

    // Alternativa valida mas cercana hacia abajo (null si no hay ninguna menor configurada)
    private Integer alternativaMenor;
    private Double precioAlternativaMenor;

    // Alternativa valida mas cercana hacia arriba (null si no hay ninguna mayor configurada)
    private Integer alternativaMayor;
    private Double precioAlternativaMayor;
}
