package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.dto;

import lombok.Data;

/** Pasar un articulo (o parte de sus piezas) de un pedido a otro cliente: exactamente uno de {@code clienteId} y {@code clienteSinRegistroId}. */
@Data
public class PasarAClienteRequest {
    /** Piezas a pasar; null = todas las de la linea. */
    private Integer cantidad;
    private Integer clienteId;
    private Integer clienteSinRegistroId;
}
