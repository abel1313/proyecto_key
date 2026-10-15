package com.ventas.key.hexagonal.precio.dominio.puerto.entrada;

import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeArticulo;

/** [Hexagonal: Driving Port] [Clean: Input Boundary] */
public interface CambiarPrecioArticuloCasoUso {

    /**
     * Precio propio para un solo articulo: los demas del producto no cambian. Con
     * {@code usarDescuento} el articulo se vende al descuento hasta que se desactive (R8).
     */
    PreciosDeArticulo cambiar(Integer varianteId, Double precioVenta, Double precioRebaja, boolean usarDescuento);

    /** Le quita el precio propio: vuelve a cobrar el del producto. */
    PreciosDeArticulo usarElDelProducto(Integer varianteId);
}
