package com.ventas.key.mis.productos.models.pedidos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PedidoQuery {
    private int id;
    private String fecha_pedido;
    private String estado_pedido;
    private String nombreReceptor;
    private Integer lugarEntregaId;
    private String lugarEntregaNombre;
    private String urlFacebook;
    private List<DetalleQuery> detalles;
}
