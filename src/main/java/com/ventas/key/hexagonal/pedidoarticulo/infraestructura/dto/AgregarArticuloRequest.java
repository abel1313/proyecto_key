package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.dto;

import lombok.Data;

/**
 * Agregar un articulo a un pedido ya creado.
 *
 * <p>[Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>No trae {@code subTotal} a proposito: el back lo calcula. Recibirlo fue el agujero por el que
 * un pedido entero podia quedar en $1 (2026-09-22).
 */
@Data
public class AgregarArticuloRequest {

    /** El articulo (lo que hoy se llama variante). */
    private Integer varianteId;

    private Integer cantidad = 1;

    /** Null = precio normal. Solo se acepta el normal o el de rebaja. */
    private Double precioUnitario;
}
