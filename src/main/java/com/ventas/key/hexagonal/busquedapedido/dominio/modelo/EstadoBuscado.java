package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Los estados como los dice la card de Mis pedidos (R3), no como los guarda la base. Desde el
 * 2026-10-06 la card dice dos cosas (dominio {@code entrega}), y el filtro igual:
 * <ul>
 *   <li><b>Pago</b>: {@code FALTA_PAGAR}, {@code PAGADO}, {@code CANCELADO}.</li>
 *   <li><b>Entrega</b>: {@code FALTA_ENTREGAR}, {@code ENTREGADO} (columna {@code pedidos.entregado};
 *       los cancelados no entran).</li>
 * </ul>
 * Dentro de cada bloque se suman (OR); entre bloques se cruzan (AND): "Pagado + Falta entregar"
 * son los que ya pagaron y no se lo han llevado.
 *
 * <p>{@code PENDIENTE} (contado sin cobrar) y {@code POR_COBRAR} (Apartado / Ir pagando abierto)
 * son los nombres de antes del 2026-10-06; se siguen aceptando por los filtros guardados, y los dos
 * son parte de "Falta pagar".
 */
public enum EstadoBuscado {
    PENDIENTE,
    POR_COBRAR,
    PAGADO,
    ENTREGADO,
    CANCELADO,
    FALTA_PAGAR,
    FALTA_ENTREGAR;

    public boolean esDeEntrega() {
        return this == ENTREGADO || this == FALTA_ENTREGAR;
    }
}
