package com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ventas.key.hexagonal.flete.dominio.modelo.LigaManualDelViaje;
import java.time.LocalDateTime;

/** Una liga hecha a mano de un viaje: {@code ligaId} es lo que se manda a {@code DELETE /v1/fletes/ligas/{ligaId}} para deshacerla. */
public record LigaManualResponse(int ligaId, int ventaId, int detalleVentaId, @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fecha,
                                 int productoId, String producto, int cantidad, double fletePorPieza) {

    public static LigaManualResponse de(LigaManualDelViaje l) {
        return new LigaManualResponse(l.ligaId(), l.ventaId(), l.detalleVentaId(), l.fechaVenta(), l.productoId(), l.producto(), l.cantidad(),
                l.fletePorPieza() == null ? 0 : l.fletePorPieza().doubleValue());
    }
}
