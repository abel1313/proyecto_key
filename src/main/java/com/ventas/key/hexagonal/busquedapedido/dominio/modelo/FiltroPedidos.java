package com.ventas.key.hexagonal.busquedapedido.dominio.modelo;

import com.ventas.key.hexagonal.busquedapedido.dominio.excepcion.FiltroInvalidoException;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Todo lo que se puede pedir en la lista de pedidos del administrador. Las reglas estan en
 * {@code README.md} (R1–R13); aqui se validan las que dependen solo del filtro.
 *
 * <p><b>R13:</b> los filtros distintos se combinan con Y (forma de cobro <i>y</i> estado <i>y</i>
 * lugar...). Dentro de un mismo filtro las opciones se combinan con O (Apartado <i>o</i> Ir
 * pagando). Un filtro vacio no filtra.
 *
 * @param texto        lo escrito en el buscador (R1), o null
 * @param formas       Contado / Apartado / Ir pagando (R2)
 * @param estados      Pendiente / Por cobrar / Pagado / Entregado / Cancelado (R3)
 * @param dinero       con saldo / sin abonos / saldo a favor (R4)
 * @param totalMinimo  total del pedido desde, en pesos (R5)
 * @param totalMaximo  total del pedido hasta, en pesos (R5)
 * @param registro     dia en que se registro el pedido, desde-hasta (R6)
 * @param entrega      atajo por fecha de entrega: hoy, mañana, esta semana, atrasados (R7)
 * @param lugarEntregaId un lugar de entrega en particular (R8)
 * @param modoEntrega  recoge en tienda / envio (R8)
 * @param unidos       solo los unidos / sin unir (R9)
 * @param soloRamos    solo pedidos de ramo de flores (R10)
 * @param soloConPromocion solo pedidos con algun articulo de promocion (R10)
 * @param orden        R11
 * @param pagina       desde 0 (R12)
 * @param tamano       de 1 a 50 (R12)
 */
public record FiltroPedidos(
        TextoBuscado texto,
        Set<FormaDeCobro> formas,
        Set<EstadoBuscado> estados,
        Set<SituacionDeDinero> dinero,
        Double totalMinimo,
        Double totalMaximo,
        RangoDeFechas registro,
        CuandoSeEntrega entrega,
        Integer lugarEntregaId,
        ModoDeEntrega modoEntrega,
        PedidosUnidos unidos,
        boolean soloRamos,
        boolean soloConPromocion,
        OrdenDePedidos orden,
        int pagina,
        int tamano) {

    public static final int TAMANO_MAXIMO = 50;

    public FiltroPedidos {
        formas = copia(formas, FormaDeCobro.class);
        estados = copia(estados, EstadoBuscado.class);
        dinero = copia(dinero, SituacionDeDinero.class);
        orden = orden == null ? OrdenDePedidos.RECIENTES : orden;

        if ((totalMinimo != null && totalMinimo < 0) || (totalMaximo != null && totalMaximo < 0)) {
            throw new FiltroInvalidoException("El total no puede ser negativo");
        }
        if (totalMinimo != null && totalMaximo != null && totalMinimo > totalMaximo) {
            throw new FiltroInvalidoException(String.format(
                    "El total \"desde\" ($%.2f) es mayor que el total \"hasta\" ($%.2f)", totalMinimo, totalMaximo));
        }
        if (pagina < 0) {
            throw new FiltroInvalidoException("La pagina empieza en 0");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new FiltroInvalidoException("Se pueden pedir de 1 a " + TAMANO_MAXIMO + " pedidos por pagina");
        }
    }

    private static <E extends Enum<E>> Set<E> copia(Collection<E> valores, Class<E> tipo) {
        if (valores == null || valores.isEmpty()) {
            return Collections.unmodifiableSet(EnumSet.noneOf(tipo));
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(valores));
    }

    /**
     * El numero exacto de un pedido unido que no es el titular tambien sale (R9): es la unica forma
     * de abrirlo desde la lista.
     */
    public Integer numeroExacto() {
        return texto == null ? null : texto.comoNumeroDePedido();
    }
}
