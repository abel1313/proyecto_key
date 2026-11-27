package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Una línea ya validada: el artículo existe y el precio salió de la base, no del modelo. */
public record LineaDePedido(int articuloId, String nombre, int cantidad, double precioUnitario) {

    public double subtotal() {
        return precioUnitario * cantidad;
    }
}
