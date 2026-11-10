package com.ventas.key.hexagonal.reportecategoria.infraestructura.dto;

import java.time.LocalDateTime;

/** Un reporte pedido: su estado (PENDIENTE, GENERANDO, LISTO, ERROR) y, si ya está LISTO, cuánto pesa el archivo para descargarlo. */
public record ReporteGeneradoResponse(int id, String estado, String formato, String descripcion, String nombreArchivo, String error,
                                      LocalDateTime creado, LocalDateTime terminado, LocalDateTime expira, long bytes) {

    public static ReporteGeneradoResponse deFila(Object[] r) {
        return new ReporteGeneradoResponse(((Number) r[0]).intValue(), (String) r[1], (String) r[2], (String) r[3], (String) r[4],
                (String) r[5], (LocalDateTime) r[6], (LocalDateTime) r[7], (LocalDateTime) r[8], ((Number) r[9]).longValue());
    }
}
