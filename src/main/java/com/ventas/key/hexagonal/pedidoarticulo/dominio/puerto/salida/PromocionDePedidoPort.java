package com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PromocionDelPedido;

import java.util.Optional;

/**
 * Que articulos componen una promocion y a que precio cada uno.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Solo lectura: este dominio edita pedidos, nunca promociones.
 */
public interface PromocionDePedidoPort {

    Optional<PromocionDelPedido> buscar(Integer promocionId);
}
