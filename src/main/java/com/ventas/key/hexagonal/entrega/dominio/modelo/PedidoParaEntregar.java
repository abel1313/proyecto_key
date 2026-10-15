package com.ventas.key.hexagonal.entrega.dominio.modelo;

/**
 * Un pedido visto desde la entrega: lo justo para decidir si se puede entregar o regresar.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * @param pedidoId       id del pedido
 * @param tipo           {@code NORMAL} (contado), {@code APARTADO} o {@code FIADO} (Ir pagando)
 * @param estado         {@code estado_pedido} tal cual: solo dice el pago
 * @param totalCentavos  total del pedido
 * @param pagadoCentavos lo que ya se abono
 * @param entregado      si ya se lo llevo
 */
public record PedidoParaEntregar(
        Integer pedidoId,
        String tipo,
        String estado,
        long totalCentavos,
        long pagadoCentavos,
        boolean entregado) {

    public boolean estaCancelado() {
        return "cancelado".equalsIgnoreCase(estado);
    }

    public boolean esIrPagando() {
        return "FIADO".equals(tipo);
    }

    /**
     * Ya no debe nada. Un contado cobrado se guarda como {@code 'Entregado'} (es el pago, no la
     * entrega) y su {@code totalPagado} puede seguir en 0; un Apartado / Ir pagando liquidado
     * queda {@code 'PAGADO'}.
     */
    public boolean estaPagado() {
        if ("NORMAL".equals(tipo)) {
            return "entregado".equalsIgnoreCase(estado);
        }
        return "PAGADO".equalsIgnoreCase(estado) || pagadoCentavos >= totalCentavos;
    }

    public long faltaCentavos() {
        return Math.max(0, totalCentavos - pagadoCentavos);
    }
}
