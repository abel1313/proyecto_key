package com.ventas.key.hexagonal.campana.dominio.puerto.salida;

/**
 * [Hexagonal: Driven Port] Las líneas de un pedido que se cobraron con precio de campaña. Sirve para avisar antes de cambiar el pedido a Ir pagando
 * y para regresarlas a su precio de antes cuando se cambia (con Ir pagando no entra el descuento).
 */
public interface PreciosDeCampanaEnPedidoPort {

    /** ¿Alguna línea del pedido lleva precio de campaña? {@code false} si las tablas todavía no existen. */
    boolean tiene(int pedidoId);

    /**
     * Regresa cada línea con precio de campaña a su precio de antes y recalcula el total del pedido.
     *
     * @return {@code true} si cambió alguna línea
     */
    boolean restaurar(int pedidoId);
}
