package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import java.time.LocalDateTime;

/**
 * Un pedido visto desde el grupo: lo justo para validar la union y repartir un abono.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * @param pedidoId      id del pedido
 * @param tipo          {@code NORMAL}, {@code APARTADO} o {@code FIADO}
 * @param estado        {@code Pendiente}, {@code APARTADO}, {@code FIADO}, {@code PAGADO},
 *                      {@code Entregado}, {@code cancelado}
 * @param totalCentavos total del pedido
 * @param pagadoCentavos lo que ya se abono
 * @param registro      cuando se hizo; ordena el reparto (el mas viejo se liquida primero)
 * @param cliente       nombre del cliente del pedido, para mostrar
 */
public record PedidoDelGrupo(
        Integer pedidoId,
        String tipo,
        String estado,
        long totalCentavos,
        long pagadoCentavos,
        LocalDateTime registro,
        String cliente) {

    private static final String ENTREGADO = "Entregado";
    private static final String CANCELADO = "cancelado";
    private static final String PAGADO = "PAGADO";

    public boolean estaCancelado() {
        return CANCELADO.equals(estado);
    }

    /** Se puede unir: no esta cancelado, ni entregado, ni liquidado (R2). */
    public boolean estaAbierto() {
        return !estaCancelado() && !estaEntregado() && !PAGADO.equals(estado);
    }

    public boolean estaPagado() {
        return PAGADO.equals(estado);
    }

    /**
     * Se puede unir: no esta cancelado ni cobrado de contado (R2).
     *
     * <p>Un pedido a credito ya liquidado si entra: su dinero pasa a ser del grupo y al separar se
     * reparte. Uno de contado ya cobrado primero se pasa a Apartado o Ir pagando.
     */
    public boolean sePuedeUnir() {
        return !estaCancelado() && !estaEntregado();
    }

    /** Por que no se puede unir, para decirselo a quien lo intento. */
    public String motivoDelCierre() {
        if (estaCancelado()) {
            return "esta cancelado";
        }
        if (estaEntregado()) {
            return "ya se cobro de contado (pasalo a Apartado o Ir pagando con \"Cambiar forma de cobro\" para unirlo)";
        }
        if (PAGADO.equals(estado)) {
            return "ya esta pagado";
        }
        return "no esta abierto";
    }

    public boolean estaEntregado() {
        return ENTREGADO.equals(estado);
    }

    /**
     * Un pedido de contado que ya se entrego queda cobrado aunque {@code totalPagado} siga en
     * cero: confirmar crea la venta pero no llena ese campo.
     */
    public long cobradoCentavos() {
        return estaEntregado() ? totalCentavos : pagadoCentavos;
    }

    /** Un pedido cancelado ya no debe nada, aunque su total diga otra cosa (R8). */
    public long saldoCentavos() {
        return estaCancelado() ? 0 : Math.max(0, totalCentavos - cobradoCentavos());
    }

    public boolean esDeCredito() {
        return "APARTADO".equals(tipo) || "FIADO".equals(tipo);
    }
}
