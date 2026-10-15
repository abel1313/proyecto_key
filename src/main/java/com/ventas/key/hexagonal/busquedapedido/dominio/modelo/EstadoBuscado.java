package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Los estados como los dice la card de Mis pedidos (R3), no como los guarda la base. Desde el
 * 2026-10-06 la card dice dos cosas (dominio {@code entrega}), y el filtro igual:
 * <ul>
 *   <li><b>Pago</b>: {@code PENDIENTE}, {@code FALTA_PAGAR}, {@code PAGADO}, {@code CANCELADO}.</li>
 *   <li><b>Entrega</b>: {@code FALTA_ENTREGAR}, {@code ENTREGADO} (columna {@code pedidos.entregado};
 *       los cancelados no entran).</li>
 * </ul>
 * Dentro de cada bloque se suman (OR); entre bloques se cruzan (AND): "Pagado + Falta entregar"
 * son los que ya pagaron y no se lo han llevado.
 *
 * <p>{@code PENDIENTE} (R16, 2026-10-13): el pedido que el cliente hizo desde su cuenta y nadie ha
 * cobrado ni pasado a Apartado / Ir pagando. Tiene su propio botón en el bloque Pago y ya
 * <b>no</b> entra en "Falta pagar": antes salía siempre revuelto ahí y no había forma de quitarlo.
 * {@code POR_COBRAR} (Apartado / Ir pagando abierto) es un nombre de antes del 2026-10-06; se sigue
 * aceptando por los filtros guardados y equivale a "Falta pagar".
 *
 * <p>{@code SIN_TERMINAR} (R15, 2026-10-13): todo lo que todavía tiene algo pendiente, o sea, menos
 * lo ya pagado y entregado y menos lo cancelado. Es la vista con la que abre Mis pedidos; lo
 * escondido se vuelve a ver con su filtro. Va aparte de los dos bloques (se cruza con ellos con Y).
 */
public enum EstadoBuscado {
    PENDIENTE,
    POR_COBRAR,
    PAGADO,
    ENTREGADO,
    CANCELADO,
    FALTA_PAGAR,
    FALTA_ENTREGAR,
    SIN_TERMINAR;

    public boolean esDeEntrega() {
        return this == ENTREGADO || this == FALTA_ENTREGAR;
    }

    /** Del bloque Pago (Falta pagar / Pagado / Cancelado y los nombres viejos). */
    public boolean esDePago() {
        return !esDeEntrega() && this != SIN_TERMINAR;
    }
}
