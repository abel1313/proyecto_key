package com.ventas.key.mis.productos.models.pedidos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PedidoDetalleResponse {
    private Integer pedidoId;
    private String tipoPedido;
    private String estadoPedido;
    private Double totalPedido;
    private Double totalPagado;
    private Double saldoPendiente;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaPedido;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaRecogida;

    private String observaciones;
    private String motivoCancelacion;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaCancelacion;

    private String clienteNombre;
    private String clienteTelefono;

    private List<DetalleItemResponse> detalles;
}
