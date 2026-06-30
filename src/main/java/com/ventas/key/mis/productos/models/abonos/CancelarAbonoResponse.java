package com.ventas.key.mis.productos.models.abonos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CancelarAbonoResponse {
    private Integer pedidoId;
    private String tipoPedido;
    private String estadoPedido;
    private Double totalPagado;
    private Double totalPendiente;
    private boolean stockDevuelto;
    private String mensaje;
}
