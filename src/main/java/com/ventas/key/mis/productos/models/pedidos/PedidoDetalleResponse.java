package com.ventas.key.mis.productos.models.pedidos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // Fecha+hora completa de la compra (para mostrar hora en ticket/detalle). En pedidos
    // anteriores a esta migración se rellena con fechaPedido a medianoche (no hay hora real).
    private LocalDateTime fechaHoraRegistro;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaRecogida;

    private String observaciones;
    private String motivoCancelacion;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaCancelacion;

    private String clienteNombre;
    private String clienteTelefono;
    private String clienteCorreo;

    // Solo para ventas NORMAL al contado (viene de la Venta ligada al pedido); null en créditos.
    private String metodoPago;
    private Double montoDado;

    private List<DetalleItemResponse> detalles;
    private List<AbonoDetalleItem> abonos;
}
