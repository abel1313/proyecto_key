package com.ventas.key.mis.productos.models.promociones;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromocionDetalleActivaDto {
    private Integer varianteId;
    private String nombreProducto;
    private String talla;
    private String color;
    private Double precioNormal;
    private Double precioEnPromocion;
    private String imagenUrl;
}
