package com.ventas.key.mis.productos.models.pedidos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EditarEntregaPedidoRequest {
    private String nombreReceptor;
    private String direccionEntrega;
    private Integer lugarEntregaId;
    private String urlFacebook;
    private LocalDate fechaEntrega;
    private String observaciones;
}
