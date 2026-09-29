package com.ventas.key.hexagonal.precio.dominio.modelo;

/**
 * Los tres precios de un producto. Sus articulos los heredan, salvo los que tienen precio propio
 * ({@link PreciosDeArticulo}, R5 del README).
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
        double rebaja = ReglasDePrecio.descuentoValido(nuevoVenta, nuevoRebaja);
        return new PreciosDeProducto(productoId, nombre, precioCosto, nuevoVenta, rebaja);
    }

    /**
     * Lo mas barato a lo que se puede cobrar: el descuento si hay, si no el normal. Desde el
     * 2026-09-29 el descuento ya no se cobra solo (lo elige el admin en el carrito), pero es el
     * que decide si puede quedar por debajo del costo.
     */
    public double precioACobrar() {
        return ReglasDePrecio.minimoCobrable(precioVenta, precioRebaja);
    }

    /**
     * Si se esta vendiendo por debajo de lo que costo. No se bloquea -- rematar es una decision
     * valida del negocio --, pero la pantalla lo avisa para que no pase por un dedo de mas.
     */
    public boolean vendeBajoCosto() {
        return precioCosto > 0 && precioACobrar() < precioCosto;
    }
}
