package com.ventas.key.mis.productos.models.variantes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndependizarVarianteResponseDto {

    private Integer productoNuevoId;
    private String codigoBarras;
    private Integer stockProductoOrigenRestante;
}
