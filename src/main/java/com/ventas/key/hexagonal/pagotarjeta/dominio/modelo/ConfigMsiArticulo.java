package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

/**
 * La casilla "acepta meses sin intereses" de un artículo.
 *
 * @param propio      lo que se marcó en el artículo; <b>null = hereda</b> la casilla de su producto (M1)
 * @param delProducto lo que tiene marcado el producto (apagado por default)
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 */
public record ConfigMsiArticulo(Boolean propio, boolean delProducto) {

    /** Lo que de verdad cuenta al cobrar: la decisión propia si la hay, si no la del producto. */
    public boolean efectivo() {
        return propio != null ? propio : delProducto;
    }

    public boolean hereda() {
        return propio == null;
    }
}
