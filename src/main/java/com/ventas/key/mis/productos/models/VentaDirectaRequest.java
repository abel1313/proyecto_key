package com.ventas.key.mis.productos.models;

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
    private int pagosYMesesId;
    private List<DetalleVentaDto> detalles;
}