package com.ventas.key.mis.productos.models.abonos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferirAbonoResponse {
    private Integer nuevoPedidoId;
    private Double totalNuevo;
    private Double montoTransferido;
    private Double saldoPendiente;
    private String estadoNuevoPedido;
    private String mensaje;
}
