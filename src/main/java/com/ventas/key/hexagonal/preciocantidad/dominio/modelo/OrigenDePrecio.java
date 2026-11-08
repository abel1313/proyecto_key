package com.ventas.key.hexagonal.preciocantidad.dominio.modelo;

/**
 * [Hexagonal: Domain] Por qué una línea de pedido se cobró con un precio menor al normal y hay que recordar el de antes: un grupo de precio
 * por cantidad (se recalcula solo al editar el pedido) o una campaña de juguetes (solo se revierte al pasar el pedido a Ir pagando).
 */
public enum OrigenDePrecio {
    GRUPO, CAMPANA
}
