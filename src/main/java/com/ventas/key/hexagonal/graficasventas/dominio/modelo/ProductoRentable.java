package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/**
 * Un producto (modelo) y lo que dejó en el periodo.
 *
 * @param margen ganancia entre lo vendido, en porcentaje (0 si no vendió nada)
 */
public record ProductoRentable(int productoId, String nombre, int piezas, double vendido, double ganancia, double margen) {
}
