package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidarCantidadRequestDto {
    private Integer tipoFlorId;
    private Integer cantidadSolicitada;
}
