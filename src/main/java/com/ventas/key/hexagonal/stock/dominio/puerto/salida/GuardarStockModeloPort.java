package com.ventas.key.hexagonal.stock.dominio.puerto.salida;

/**
 * Escribe el stock total de un modelo. Solo eso: no valida (lo hace el modelo de dominio) ni
 * invalida caches (lo hace {@link AvisarCambioStockPort}).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface GuardarStockModeloPort {

    /**
     * Cambia el total de {@code stockAnterior} a {@code stockNuevo} solo si el modelo sigue teniendo
     * {@code stockAnterior}: si en medio otra operacion le movio el stock (editar un pedido, otro
     * ajuste), no escribe y devuelve false, para no perder ese movimiento.
     */
    boolean guardarStockTotal(Integer productoId, int stockAnterior, int stockNuevo);
}
