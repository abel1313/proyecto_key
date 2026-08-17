package com.ventas.key.mis.productos.models.floreseternas;

import lombok.Data;

import java.util.List;

// Reemplaza la composicion del ramo (colores y accesorios) de un pedido ya existente -- ver
// RamoPedidoDetalleServiceImpl.editarRamo(). No toca fecha de entrega, urgencia, envio ni el
// liston: eso queda fuera de esta primera version por la complejidad de re-disparar sus propias
// reglas (aprobacion de frase, recalculo de urgencia). Solo ADMIN.
@Data
public class EditarRamoRequestDto {
    private List<ColorSeleccionadoDto> colores;
    private List<AccesorioSeleccionadoDto> accesorios;
}
