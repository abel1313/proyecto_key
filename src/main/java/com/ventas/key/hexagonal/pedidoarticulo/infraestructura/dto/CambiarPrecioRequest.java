package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.dto;

import lombok.Data;

/**
 * Cambiar SOLO el precio de una linea de un pedido (decision del administrador; el articulo y la cantidad no cambian).
 *
 * <p>[Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 */
@Data
public class CambiarPrecioRequest {

    /** El precio nuevo por pieza; mayor a cero. */
    private Double precioUnitario;
}
