package com.ventas.key.mis.productos.dto.variantes;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class RequestVarianteDto {

    private Integer productoId;
    private int cantidadVariantes;
    private boolean imagenParaTodas;
}
