package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RamoPedidoDetalleColorDto {
    private Integer colorFlorId;
    private String colorNombre;
    private Integer cantidad;
}
