package com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoAlGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;

import java.util.List;

/**
 * Cambiar la forma de cobro de todos los pedidos de un grupo a la vez (R20, A10 del plan).
 *
 * <p>[Hexagonal: Driving Port] [Clean: Use Case input boundary]
 *
 * <p>Va aparte de {@link UnirPedidosCasoUso}: lo usa para leer el grupo y para el abono, y asi
 * cada clase se queda con un solo trabajo.
 */
public interface CambiarFormaDeCobroGrupoCasoUso {

    /**
     * Todo o nada: si un pedido no se puede cambiar, ninguno cambia.
     *
     * @param tipoNuevo {@code APARTADO} o {@code FIADO}
     * @param cobro     lo que el cliente deja ahora (solo Ir pagando), o null; entra como abono del grupo (R6)
     */
    Resultado cambiar(Integer grupoId, String tipoNuevo, AbonoAlGrupo cobro, String nota, Integer usuarioId);

    /**
     * @param grupo            el grupo despues del cambio
     * @param tipoAnterior     la forma de cobro que tenia
     * @param pedidosCambiados los que cambiaron, del mas viejo al mas nuevo
     * @param abono            el abono que se dio en el mismo paso, o null
     */
    record Resultado(GrupoPedidos grupo, String tipoAnterior, List<Integer> pedidosCambiados,
                     UnirPedidosCasoUso.ResultadoAbono abono) {
    }
}
