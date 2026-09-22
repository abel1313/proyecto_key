package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.dto;

import lombok.Data;

/**
 * Cambiar una linea de un pedido por otro articulo.
 *
 * <p>[Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 */
@Data
public class CambiarArticuloRequest {

    /** El articulo nuevo. */
    private Integer varianteId;

    /** Null = se conserva la cantidad que ya tenia la linea. */
    private Integer cantidad;

    /** Null = precio normal; si el articulo nuevo sigue dentro del combo, manda el del combo. */
    private Double precioUnitario;

    /**
     * Que hacer si el articulo nuevo no pertenece a la promocion de esta linea.
     *
     * <p>{@code VALIDAR} (o null) = no cambiar nada y devolver las dos opciones para que el
     * usuario elija. {@code QUITAR_PROMOCION} = sale el combo entero. {@code CONSERVAR_PROMOCION}
     * = la promocion queda y el articulo nuevo se suma aparte.
     */
    private String modo;
}
