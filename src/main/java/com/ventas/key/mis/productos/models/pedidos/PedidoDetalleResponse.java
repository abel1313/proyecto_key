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

    // Hora y punto de encuentro del viaje semanal a la zona (solo si "Entregas por zona" ya
    // programó este pedido -- ver EntregaZonaServiceImpl.programarEntrega). Null hasta que se
    // programe, o si el pedido no es de una zona con viaje semanal (recoger en tienda / ramo).
    private String horaRecogida;
    private String puntoEncuentro;

    // Punto exacto del encuentro marcado en el mapa al programar el viaje (2026-09-09). Es A
    // DONDE TIENE QUE IR EL CLIENTE -- distinto de latitud/longitud de más abajo, que son las
    // coordenadas de su propia casa. Null si el viaje se programó sin marcar el mapa.
    private Double latitudEncuentro;
    private Double longitudEncuentro;

    // Fecha+hora exacta de entrega de un ramo de flores eternas (RamoPedidoDetalle.fechaHoraEntrega)
    // -- antes solo se guardaba y nunca se le mostraba al cliente en su propio pedido (2026-09-08).
    // Null si el pedido no es de flores o todavía no tiene fecha elegida.
    private LocalDateTime fechaHoraEntregaRamo;

    private String observaciones;
    private String motivoCancelacion;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaCancelacion;

    private String nombreReceptor;
    private String direccionEntrega;
    private Double latitud;
    private Double longitud;
    private String referencias;
    private Integer lugarEntregaId;
    private String lugarEntregaNombre;
    private String urlFacebook;

    private String clienteNombre;
    private String clienteTelefono;
    private String clienteCorreo;

    // Solo para ventas NORMAL al contado (viene de la Venta ligada al pedido); null en créditos.
    private String metodoPago;
    private Double montoDado;

    private List<DetalleItemResponse> detalles;
    private List<AbonoDetalleItem> abonos;
    // true si este pedido tiene un ramo de flores eternas asociado (RamoPedidoDetalle) -- el
    // front lo usa para decidir si vale la pena buscar el detalle de flores aparte
    // (GET /v1/flores/pedidos/{id}/detalle) en vez de intentarlo siempre.
    private Boolean esRamoFlores;
}
