package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

/** Un renglon de un pedido de prueba: que articulo, cuantas piezas y a que precio del catalogo. */
public record LineaDePedido(int articuloId, int cantidad, double precio) {

    public double subtotal() {
        return precio * cantidad;
    }
}
