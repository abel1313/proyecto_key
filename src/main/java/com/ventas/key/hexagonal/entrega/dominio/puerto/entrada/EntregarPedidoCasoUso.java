package com.ventas.key.hexagonal.entrega.dominio.puerto.entrada;

import java.util.List;

/**
 * [Hexagonal: Driving Port] [Clean: Use Case Input Boundary]
 *
 * <p>Marcar que el cliente ya se llevo su pedido (o todo su grupo), y deshacerlo.
 */
public interface EntregarPedidoCasoUso {

    /** @param pedidos los pedidos que cambiaron; {@code entregado} como quedaron */
    record ResultadoEntrega(List<Integer> pedidos, boolean entregado) {}

    ResultadoEntrega entregar(Integer pedidoId);

    ResultadoEntrega regresar(Integer pedidoId);
}
