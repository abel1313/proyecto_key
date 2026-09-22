package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.dto;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDePedido;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PedidoEditable;

import java.util.List;

/**
 * Como quedo el pedido despues de la edicion.
 *
 * <p>[Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>Trae lo justo para repintar la lista de articulos y el resumen de dinero sin volver a pedir
 * el detalle completo.
 *
 * @param saldo puede dar <b>negativo</b> si el pedido ya tenia abonos y ahora vale menos: eso es
 *              dinero a favor del cliente y la pantalla tiene que mostrarlo, no esconderlo (R7)
 */
public record PedidoArticulosResponse(
        Integer pedidoId,
        String estadoPedido,
        double totalPedido,
        double totalPagado,
        double saldo,
        List<LineaResponse> articulos) {

    public static PedidoArticulosResponse de(PedidoEditable pedido) {
        double total = pedido.total();
        return new PedidoArticulosResponse(
                pedido.pedidoId(),
                pedido.estado(),
                total,
                pedido.totalPagado(),
                total - pedido.totalPagado(),
                pedido.articulos().stream().map(LineaResponse::de).toList());
    }

    /**
     * Una linea.
     *
     * @param detalleId es lo que hay que mandar para cambiar o quitar esta linea
     */
    public record LineaResponse(
            Integer detalleId,
            Integer varianteId,
            Integer productoId,
            String nombre,
            int cantidad,
            double precioUnitario,
            double subTotal,
            Integer promocionId,
            boolean esDePromocion) {

        public static LineaResponse de(ArticuloDePedido a) {
            return new LineaResponse(
                    a.detalleId(), a.varianteId(), a.productoId(), a.nombre(),
                    a.cantidad(), a.precioUnitario(), a.subTotal(),
                    a.promocionId(), a.esDePromocion());
        }
    }
}
