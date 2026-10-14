package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

/**
 * Una venta de contado hecha a un cliente sin registro con correo: la venta genera el código que el
 * cliente captura en "Agregar mi compra".
 *
 * @param pedidoId el pedido de esa venta
 * @param codigo   el código de "Agregar mi compra"
 */
public record CompraConCodigo(int pedidoId, String codigo) {
}
