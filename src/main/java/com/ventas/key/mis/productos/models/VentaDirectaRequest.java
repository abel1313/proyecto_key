package com.ventas.key.mis.productos.models;

import com.ventas.key.mis.productos.dto.ClienteSinRegistroDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VentaDirectaRequest {
    private int usuarioId;
    private int clienteId;
    private Integer pagosYMesesId; // null cuando tipoPedido = APARTADO | FIADO
    private ClienteSinRegistroDto clienteSinRegistroDto;
    private List<DetalleVentaDto> detalles;
    private String tipoPedido; // null/"NORMAL" = venta inmediata; "APARTADO"|"FIADO" = crédito
    private String observaciones;
}