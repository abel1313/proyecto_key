package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

/** Un articulo del pedido visto desde el cobro: solo importa si acepta meses sin intereses (M1). */
public record ArticuloACobrar(Integer varianteId, String nombre, boolean aceptaMsi) {
}
