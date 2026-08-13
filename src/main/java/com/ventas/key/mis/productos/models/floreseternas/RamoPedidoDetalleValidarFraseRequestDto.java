package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Usado por el ADMIN para cerrar el ciclo de una frase personalizada pendiente de validar:
// aprobarla (con el precio que le va a poner) o rechazarla, y marcar si ya se pago el anticipo.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RamoPedidoDetalleValidarFraseRequestDto {
    private Boolean aprobar;
    private Double precioAsignado;
    private Boolean anticipoPagado;
}
