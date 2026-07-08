package com.ventas.key.mis.productos.models.promociones;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromocionDetalleRequestDto {
    private Integer varianteId;
    private Integer cantidad;
    private Double precioEnPromocion;
}
