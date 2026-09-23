package com.ventas.key.hexagonal.precio.dominio.puerto.entrada;

import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeProducto;

/** [Hexagonal: Driving Port] [Clean: Input Boundary] */
public interface CambiarPrecioCasoUso {

    /** Cambia el precio normal y el de descuento del producto, para todos sus articulos. */
    PreciosDeProducto cambiar(Integer productoId, Double precioVenta, Double precioRebaja);
}
