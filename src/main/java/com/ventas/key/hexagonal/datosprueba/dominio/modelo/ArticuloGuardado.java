package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

/** Un articulo ya guardado: lo que hace falta para venderlo en un pedido de prueba. */
public record ArticuloGuardado(int id, double precio, int stock) {
}
