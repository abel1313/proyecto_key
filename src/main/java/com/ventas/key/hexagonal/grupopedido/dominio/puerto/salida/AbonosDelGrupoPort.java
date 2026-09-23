package com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoRegistrado;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.Movimiento;

import java.util.Collection;
import java.util.List;

/**
 * Los abonos de los pedidos del grupo, para repartirlos al separar.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface AbonosDelGrupoPort {

    List<AbonoRegistrado> abonosDe(Collection<Integer> pedidoIds);

    /** Pasa el abono (o una parte, partiendolo) al otro pedido, con la misma fecha y forma de pago. */
    void mover(Movimiento movimiento, String nota);
}
