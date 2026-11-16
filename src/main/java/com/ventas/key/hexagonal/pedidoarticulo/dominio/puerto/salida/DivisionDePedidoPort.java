package com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.entrada.DividirPedidoCasoUso.DestinoCliente;

/**
 * [Hexagonal: Driven Port] Lo que hace falta de la base para dividir un pedido.
 */
public interface DivisionDePedidoPort {

    /** Bloquea la fila del pedido de origen hasta el fin de la transaccion (evita dos movimientos o un abono a la vez). Debe ser lo primero que se lee. */
    void bloquear(Integer pedidoId);

    /** ¿El pedido esta unido a otros en un grupo? (un pedido unido no se divide: sus abonos son del grupo). */
    boolean estaEnGrupoActivo(Integer pedidoId);

    /** ¿Ese cliente existe y es distinto del que ya tiene el pedido de origen? */
    boolean destinoValido(Integer pedidoOrigenId, DestinoCliente destino);

    /**
     * Crea un pedido nuevo, vacio, para ese cliente, con la misma forma de cobro y si ya se entrego (la direccion/fecha de entrega no se copian: son del otro cliente) que el de origen y una nota que dice de cual vino.
     * Devuelve su id.
     */
    int crearPedidoParaCliente(Integer pedidoOrigenId, DestinoCliente destino, String nota);
}
