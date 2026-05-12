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
    private int pagosYMesesId;
    private ClienteSinRegistroDto clienteSinRegistroDto;
    private List<DetalleVentaDto> detalles;
}