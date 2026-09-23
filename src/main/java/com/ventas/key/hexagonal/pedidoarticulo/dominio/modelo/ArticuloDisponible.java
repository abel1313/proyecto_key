package com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo;

/**
 * Un articulo del catalogo y cuanto hay de el, al momento de intentar meterlo en un pedido.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Junta el stock de la variante con el del producto padre porque el sistema descuenta de los
 * dos y una edicion tiene que respetar ambos: alcanzar en la variante pero no en el producto deja
 * el inventario en negativo, que es el estado del que se salio el 2026-09-22.
 *
 * @param varianteId     el articulo
 * @param productoId     su modelo
 * @param nombre         para los mensajes
 * @param modeloHabilitado  si el producto padre esta deshabilitado, ninguno de sus articulos se vende
 * @param habilitado     una variante dada de baja no se puede vender
 * @param stockVariante  piezas de esta talla/color
 * @param stockProducto  piezas del modelo
 * @param precios        a que se puede cobrar (R2)
 */
public record ArticuloDisponible(
        Integer varianteId,
        Integer productoId,
        String nombre,
        boolean modeloHabilitado,
        boolean habilitado,
        int stockVariante,
        int stockProducto,
        PrecioCatalogo precios) {

    /**
     * Si alcanza para llevarse esta cantidad.
     *
     * <p>Manda el menor de los dos: el sistema descuenta de la variante y del producto, asi que
     * el que primero se acabe es el que limita.
     */
    public boolean alcanzaPara(int cantidad) {
        return disponible() >= cantidad;
    }

    public int disponible() {
        return Math.min(stockVariante, stockProducto);
    }

    /** Cual de los dos se queda corto, para que el error diga donde mirar. */
    public String detalleDelFaltante(int cantidad) {
        return String.format(
                "Solicitado: %d. Disponible: %d (articulo: %d, modelo: %d)",
                cantidad, disponible(), stockVariante, stockProducto);
    }
}
