package com.ventas.key.hexagonal.cortecaja.dominio.puerto.salida;

import com.ventas.key.hexagonal.cortecaja.dominio.modelo.PedidoPorCobrar;

import java.util.List;

/** [Hexagonal: Driven Port] [Clean: Gateway] Los pedidos que no están entregados ni cancelados. Solo lee. */
public interface PedidosAbiertosPort {

    /** Pedidos sin entregar y sin cancelar (con o sin saldo: el dominio decide cuáles tienen algo por cobrar). */
    List<PedidoPorCobrar> sinEntregar();
}
