package com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoAlGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.Reparto;

import java.util.List;
import java.util.Optional;

/**
 * Unir pedidos para cobrarlos y entregarlos juntos, y deshacerlo.
 *
 * <p>[Hexagonal: Driving Port] [Clean: Use Case input boundary]
 */
public interface UnirPedidosCasoUso {

    GrupoPedidos unir(List<Integer> pedidoIds, Integer pedidoTitularId, String nota, Integer usuarioId);

    GrupoPedidos consultar(Integer grupoId);

    /** El grupo activo del pedido, si esta en uno. */
    Optional<GrupoPedidos> grupoActivoDe(Integer pedidoId);

    ResultadoAbono abonar(Integer grupoId, AbonoAlGrupo abono, Integer usuarioId);

    /**
     * Cobra de una vez todos los pedidos de contado del grupo que falten, con la misma forma de
     * pago (R11). Todo o nada: si uno falla, ninguno queda cobrado.
     */
    ResultadoCobro cobrarDeContado(Integer grupoId, Integer pagosYMesesId, Integer usuarioId);

    /** Deshace el grupo. Cada pedido se queda con sus articulos y los abonos que le tocaron (R7). */
    GrupoPedidos deshacer(Integer grupoId, String motivo, Integer usuarioId);

    /**
     * @param grupo          el grupo despues del abono
     * @param repartos       cuanto le toco a cada pedido
     * @param cambioCentavos lo que hay que regresarle al cliente
     */
    record ResultadoAbono(GrupoPedidos grupo, List<Reparto> repartos, long cambioCentavos) {
    }

    /**
     * @param grupo           el grupo despues del cobro
     * @param pedidosCobrados los que se confirmaron, del mas viejo al mas nuevo
     */
    record ResultadoCobro(GrupoPedidos grupo, List<Integer> pedidosCobrados) {
    }
}
