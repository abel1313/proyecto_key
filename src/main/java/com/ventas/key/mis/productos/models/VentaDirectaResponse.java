package com.ventas.key.mis.productos.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
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
}