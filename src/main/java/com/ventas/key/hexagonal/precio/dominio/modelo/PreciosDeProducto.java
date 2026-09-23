package com.ventas.key.hexagonal.precio.dominio.modelo;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;

/**
 * Los tres precios de un producto. Todos sus articulos los heredan (R3 de `articulo`).
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * @param precioCosto  lo que costo; nunca se vende a esto, solo sirve para avisar
 * @param precioVenta  el normal al cliente
 * @param precioRebaja el precio con descuento; 0 = sin descuento. Es un precio de catalogo valido
 *                     para cobrar (ver PrecioCatalogo en `pedidoarticulo`)
 */
public record PreciosDeProducto(
        Integer productoId,
        String nombre,
        double precioCosto,
        double precioVenta,
        double precioRebaja) {

    /**
     * Los precios nuevos, validados (R1 y R2 del README).
     *
     * <p>Nace de un caso real (2026-09-22): a un producto le bajaron el precio y, como no habia
     * forma rapida de cambiarlo, se armo una promocion solo para eso -- y la promocion se registro
     * como pago en efectivo cuando el cliente iba a ir pagando.
     */
    public PreciosDeProducto conPrecios(Double nuevoVenta, Double nuevoRebaja) {
        if (nuevoVenta == null || nuevoVenta <= 0) {
            throw PrecioInvalidoException.ventaObligatoria();
        }
        double rebaja = nuevoRebaja == null ? 0.0 : nuevoRebaja;
        if (rebaja < 0) {
            throw PrecioInvalidoException.rebajaNegativa();
        }
        if (rebaja > nuevoVenta) {
            throw PrecioInvalidoException.rebajaMayorQueVenta(rebaja, nuevoVenta);
        }
        return new PreciosDeProducto(productoId, nombre, precioCosto, nuevoVenta, rebaja);
    }

    /** El precio al que se cobra hoy: el descuento si hay, si no el normal. */
    public double precioACobrar() {
        return precioRebaja > 0 ? precioRebaja : precioVenta;
    }

    /**
     * Si se esta vendiendo por debajo de lo que costo. No se bloquea -- rematar es una decision
     * valida del negocio --, pero la pantalla lo avisa para que no pase por un dedo de mas.
     */
    public boolean vendeBajoCosto() {
        return precioCosto > 0 && precioACobrar() < precioCosto;
    }
}
