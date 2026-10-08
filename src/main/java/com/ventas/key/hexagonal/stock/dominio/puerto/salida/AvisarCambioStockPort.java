package com.ventas.key.hexagonal.stock.dominio.puerto.salida;

/**
 * Avisa que el stock de un modelo cambio, para que los listados no sigan mostrando el valor viejo
 * (caches de Redis y del micro de imagenes).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface AvisarCambioStockPort {

    void stockCambio(Integer productoId);
}
