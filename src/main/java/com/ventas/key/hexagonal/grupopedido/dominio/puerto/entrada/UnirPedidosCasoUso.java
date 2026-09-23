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

    /**
     * Separa uno o varios pedidos. En un grupo a credito, lo que el cliente ha dado se reparte como
     * se indique; la suma tiene que ser exacta (R14). Si quedan 2 o mas, siguen unidos en un grupo
     * nuevo (R15).
     *
     * @param reparto pedidoId -> centavos que se queda cada pedido que sale
     */
    ResultadoSeparacion separar(Integer grupoId, List<Integer> salen, java.util.Map<Integer, Long> reparto,
                                Integer nuevoTitular, String motivo, Integer usuarioId);

    /** Cambia quien paga y recoge (R16). */
    GrupoPedidos cambiarTitular(Integer grupoId, Integer pedidoTitularId, Integer usuarioId);

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
    /**
     * @param grupo       el grupo que se separo (ya inactivo), con como quedo cada pedido
     * @param grupoNuevo  id del grupo en el que siguen unidos los demas, o null
     */
    record ResultadoSeparacion(GrupoPedidos grupo, Integer grupoNuevo) {
    }

    record ResultadoCobro(GrupoPedidos grupo, List<Integer> pedidosCobrados) {
    }
}
