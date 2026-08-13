package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColorCalculadoDto {
    private Integer colorFlorId;
    private String colorNombre;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
    // Variante "sombra" de ESTE color -- usarla para armar su propia linea en savePedido.
    private Integer varianteId;
}
