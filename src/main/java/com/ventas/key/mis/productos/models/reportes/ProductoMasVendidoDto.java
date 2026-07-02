package com.ventas.key.mis.productos.models.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoMasVendidoDto {
    private Integer varianteId;
    private String productoNombre;
    private String talla;
    private String color;
    private Long cantidadVendida;
    private Double totalVendido;
}
