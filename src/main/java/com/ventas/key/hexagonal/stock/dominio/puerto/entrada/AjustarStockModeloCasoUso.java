package com.ventas.key.hexagonal.stock.dominio.puerto.entrada;

import com.ventas.key.hexagonal.stock.dominio.modelo.DisponibilidadStock;

/**
 * Agregar (+) o quitar (-) stock a un modelo sin dar de alta un articulo (Agregar articulo,
 * 2026-10-08): lo agregado queda libre y la pantalla ofrece dar de alta sus articulos de una vez.
 *
 * <p>[Hexagonal: Driving Port] [Clean: Use Case Input Boundary]
 */
public interface AjustarStockModeloCasoUso {

    /** La disponibilidad del modelo ya con el ajuste guardado. */
    DisponibilidadStock ajustar(Integer productoId, int ajuste);
}
