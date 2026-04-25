package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PagoMPRequest {
    private Integer pedidoId;
    private Integer clienteId;
    private Integer pagosYMesesId;
    private Integer cuotas;
    private Double totalMonto;
    private String descripcion;
}