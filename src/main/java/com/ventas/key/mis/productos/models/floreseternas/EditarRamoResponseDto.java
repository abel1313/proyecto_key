package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditarRamoResponseDto {
    private RamoPedidoDetalleResponseDto ramo;
    private Double totalPedidoAnterior;
    private Double totalPedidoNuevo;
    // nuevo - anterior. Positivo = el cliente debe pagar mas (registrar abono aparte via
    // POST /v1/abonos/{pedidoId}); negativo o cero = no debe nada mas.
    private Double diferencia;
}
