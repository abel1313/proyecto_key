package com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.entrada;

/**
 * Dividir un pedido: pasar un articulo (o parte de sus piezas) a OTRO cliente, en un pedido nuevo.
 *
 * <p>[Hexagonal: Driving Port] [Clean: Use Case]
 *
 * <p>Solo pedidos en Apartado o Ir pagando sin liquidar. El pedido nuevo nace con la misma forma de cobro, sin abonos; los abonos se quedan en el
 * pedido de origen, y se rechaza el movimiento si ese pedido quedaria con dinero de mas. El stock no cambia: solo cambia de pedido.
 */
public interface DividirPedidoCasoUso {

    /**
     * @param cantidad piezas a mover; {@code null} = todas las de la linea
     * @param destino  el otro cliente (registrado o sin registro)
     * @return el id del pedido nuevo
     */
    int pasarArticuloACliente(Integer pedidoId, Integer detalleId, Integer cantidad, DestinoCliente destino);

    /** Exactamente uno de los dos ids. */
    record DestinoCliente(Integer clienteId, Integer clienteSinRegistroId) {
        public boolean esValido() {
            return (clienteId != null) != (clienteSinRegistroId != null);
        }
    }
}
