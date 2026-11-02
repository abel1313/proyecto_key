package com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida;

/**
 * Dice si un pedido tiene un cobro con tarjeta en proceso, o ya cobrado y sin devolver del todo.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Si lo tiene, el pedido no se une a un grupo: el grupo no se cobra con tarjeta y el cobro quedaria huerfano.
 */
public interface CobroConTarjetaPort {

    boolean tieneCobroConTarjeta(Integer pedidoId);
}
