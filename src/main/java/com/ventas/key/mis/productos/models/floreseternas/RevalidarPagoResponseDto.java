package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta de RamoPedidoDetalleServiceImpl.revalidarAntesDePagar -- el front la llama justo
// antes de POST /v1/abonos/{pedidoId} para saber si el plazo urgente ya vencio.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevalidarPagoResponseDto {
    // true solo cuando el cargo se acaba de aplicar en ESTA llamada (pago fuera de tiempo).
    // false = todo normal (no era urgente, ya se habia aplicado antes, o sigue a tiempo).
    private Boolean cargoRecienAplicado;
    // Monto del cargo que se acaba de agregar -- null si cargoRecienAplicado es false.
    private Double cargoAgregado;
    // Total actual del pedido DESPUES de esta llamada (ya con el cargo si aplico). El front debe
    // usar este valor, no uno que haya calculado antes, para el monto del abono.
    private Double totalActual;
    // Mensaje para mostrarle al cliente, solo presente cuando cargoRecienAplicado es true.
    private String mensaje;
}
