package com.ventas.key.mis.productos.models.pedidos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta de {@code GET /v1/entregas-zona/{lugarEntregaId}/pendientes}.
 *
 * {@code desde}/{@code hasta} son el rango de fecha de pedido que realmente se está mostrando.
 * Antes la pantalla solo podía ver la semana en curso (lunes a viernes, calculada en el back);
 * ahora el admin elige el rango con dos calendarios y estos dos campos confirman cuál se aplicó
 * -- si no manda rango, siguen siendo la semana actual, como antes.
 *
 * {@code lunes}/{@code viernes} se conservan con el mismo valor que desde/hasta solo para no
 * romper a quien todavía los lea; usar desde/hasta.
 *
 * {@code fechaSugerida} viene de {@code LugarEntrega.diaEntregaSemanal} ya resuelto a una fecha
 * concreta (o null si esa zona no tiene día configurado todavía).
 */
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class EntregaZonaSemanaResponse {
    private LocalDate desde;
    private LocalDate hasta;
    /** @deprecated usar {@link #desde}. Se mantiene por compatibilidad. */
    @Deprecated
    private LocalDate lunes;
    /** @deprecated usar {@link #hasta}. Se mantiene por compatibilidad. */
    @Deprecated
    private LocalDate viernes;
    private LocalDate fechaSugerida;
    private List<EntregaZonaPendienteDto> pedidos;

    public EntregaZonaSemanaResponse(LocalDate desde, LocalDate hasta, LocalDate fechaSugerida,
                                     List<EntregaZonaPendienteDto> pedidos) {
        this.desde = desde;
        this.hasta = hasta;
        this.lunes = desde;
        this.viernes = hasta;
        this.fechaSugerida = fechaSugerida;
        this.pedidos = pedidos;
    }
}
