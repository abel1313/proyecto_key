package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VentaDirectaResponse {
    private Integer ventaId;
    private String tipoPago;
    private boolean requiereTerminal;
    private Double totalVenta;
    private String meses;
    private String descripcionPago;
    private String intentId;
}