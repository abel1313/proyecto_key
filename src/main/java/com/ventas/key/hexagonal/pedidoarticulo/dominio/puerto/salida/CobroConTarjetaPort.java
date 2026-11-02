package com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida;

/**
 * Dice si un pedido tiene un cobro con tarjeta en proceso, o ya cobrado y sin devolver del todo.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Si lo tiene, no se le agregan, quitan ni cambian articulos: el total cambiaria con el dinero ya en la tarjeta del cliente.
 */
public interface CobroConTarjetaPort {

    boolean tieneCobroConTarjeta(Integer pedidoId);
}
