package com.ventas.key.hexagonal.precio.dominio.puerto.entrada;

import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeArticulo;

/** [Hexagonal: Driving Port] [Clean: Input Boundary] */
public interface CambiarPrecioArticuloCasoUso {

    /** Precio propio para un solo articulo: los demas del producto no cambian. */
    PreciosDeArticulo cambiar(Integer varianteId, Double precioVenta, Double precioRebaja);

    /** Le quita el precio propio: vuelve a cobrar el del producto. */
    PreciosDeArticulo usarElDelProducto(Integer varianteId);
}
