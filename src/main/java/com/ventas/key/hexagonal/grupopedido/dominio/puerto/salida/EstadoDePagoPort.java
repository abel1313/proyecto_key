package com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida;

/**
 * Deja el pedido a credito como corresponde a los abonos que tiene despues de repartir.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Si ya cubren el total queda PAGADO y se crea su venta; si ya no lo cubren (se le paso dinero
 * a otro pedido) vuelve a Apartado / Ir pagando y se borra la venta que tenia.
 */
public interface EstadoDePagoPort {

    void ajustarAlosAbonos(Integer pedidoId, Integer usuarioId);
}
