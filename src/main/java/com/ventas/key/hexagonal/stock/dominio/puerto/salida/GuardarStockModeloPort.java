package com.ventas.key.hexagonal.stock.dominio.puerto.salida;

/**
 * Escribe el stock total de un modelo. Solo eso: no valida (lo hace el modelo de dominio) ni
 * invalida caches (lo hace {@link AvisarCambioStockPort}).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface GuardarStockModeloPort {

    void guardarStockTotal(Integer productoId, int stockTotal);
}
