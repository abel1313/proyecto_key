package com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida;

/**
 * Registra un abono en un pedido con el mismo camino que el abono normal.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>No se reescribe el cobro: el abono normal ya valida el saldo, pasa el pedido a PAGADO y
 * crea la venta al liquidar. Dos caminos que cobran serian dos caminos que cobran distinto.
 */
public interface AbonoPedidoPort {

    void abonar(Integer pedidoId, long montoCentavos, String metodoPago, String nota, Integer usuarioId);
}
