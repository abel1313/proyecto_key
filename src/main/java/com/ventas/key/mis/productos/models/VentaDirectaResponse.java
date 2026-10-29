package com.ventas.key.mis.productos.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VentaDirectaResponse {
    private Integer ventaId;
    private String tipoPago;
    private boolean requiereTerminal;
    private Double totalVenta;
    private String meses;
    private String descripcionPago;
    private String intentId;
    private Integer pedidoId; // solo cuando tipoPedido=APARTADO|FIADO; ventaId será null
    private Boolean correoEnviado;
    private Boolean whatsappEnviado;
    private List<String> erroresEnvio;
    /** true: la venta quedó como pedido Pendiente y se cobra por /v1/pagos-tarjeta (módulo nuevo) con el pedidoId. */
    private Boolean cobroConModuloNuevo;

    public VentaDirectaResponse(Integer ventaId, String tipoPago, boolean requiereTerminal, Double totalVenta,
                                String meses, String descripcionPago, String intentId, Integer pedidoId,
                                Boolean correoEnviado, Boolean whatsappEnviado, List<String> erroresEnvio) {
        this.ventaId = ventaId;
        this.tipoPago = tipoPago;
        this.requiereTerminal = requiereTerminal;
        this.totalVenta = totalVenta;
        this.meses = meses;
        this.descripcionPago = descripcionPago;
        this.intentId = intentId;
        this.pedidoId = pedidoId;
        this.correoEnviado = correoEnviado;
        this.whatsappEnviado = whatsappEnviado;
        this.erroresEnvio = erroresEnvio;
    }
}