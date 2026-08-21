package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Resultado interno de FlorPedidoServiceImpl.validarAnticipacionYUrgencia -- no es un DTO de
// request/response HTTP, es el valor de retorno compartido entre FlorPedidoServiceImpl
// (calcular-precio) y RamoPedidoDetalleServiceImpl (adjuntar detalle), para que ambos evaluen la
// misma regla en servidor sin duplicar logica.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnticipacionResultadoDto {
    private boolean entregaValida;
    private String mensajeEntrega;
    private Double precioUrgencia;
}
