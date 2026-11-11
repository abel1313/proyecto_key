package com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ventas.key.hexagonal.flete.dominio.modelo.LineaSinViaje;

import java.time.LocalDateTime;

/** Una línea de venta con piezas todavía sin ligar a un viaje: {@code pendientes} de {@code cantidad}. */
public record LineaSinViajeResponse(int detalleVentaId, int ventaId, @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fecha,
                                    int cantidad, int pendientes, double precioUnitario) {

    public static LineaSinViajeResponse de(LineaSinViaje l) {
        return new LineaSinViajeResponse(l.detalleVentaId(), l.ventaId(), l.fecha(), l.cantidad(), l.pendientes(),
                l.precioUnitario() == null ? 0 : l.precioUnitario().doubleValue());
    }
}
