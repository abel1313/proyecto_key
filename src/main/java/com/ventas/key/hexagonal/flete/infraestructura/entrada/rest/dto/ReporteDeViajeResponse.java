package com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ventas.key.hexagonal.flete.dominio.modelo.ReporteDeViaje;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cómo va un viaje. {@code estado}: ABIERTO, FLETE_CUBIERTO o VENDIDO_POR_COMPLETO. {@code fleteRecuperado} = lo que lo vendido ya recuperó del costo a cubrir;
 * {@code faltaPorCubrir}; {@code fleteComoGanancia} = flete de lo vendido DESPUÉS de cubrirlo (ya es ganancia); {@code pagadoConVentaId}/{@code pagadoEn} = la venta
 * con la que se cubrió (null si aún no).
 */
public record ReporteDeViajeResponse(Integer viajeId, String nombre, @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fecha, String estado,
                                     double fleteTotal, double costoACubrir, double fleteRecuperado, double faltaPorCubrir, double fleteComoGanancia,
                                     Integer pagadoConVentaId, @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime pagadoEn,
                                     int piezasCompradas, int piezasVendidas, int piezasRestantes,
                                     double totalVendido, double costoVendido, double gananciaVendida, List<Renglon> renglones) {

    public record Renglon(int productoId, String nombre, String codigoBarras, int piezasCompradas, int piezasVendidas, int piezasRestantes, double totalVendido) {
    }

    public static ReporteDeViajeResponse de(ReporteDeViaje r) {
        var v = r.viaje();
        return new ReporteDeViajeResponse(v.id(), v.nombre(), v.fecha(), r.estado(), v.fleteTotal().doubleValue(), v.costoACubrir().doubleValue(),
                r.fleteRecuperado().doubleValue(), r.faltaPorCubrir().doubleValue(), r.fleteComoGanancia().doubleValue(), r.pagadoConVentaId(), r.pagadoEn(),
                r.piezasCompradas(), r.piezasVendidas(), r.piezasRestantes(), r.totalVendido().doubleValue(), r.costoVendido().doubleValue(),
                r.gananciaVendida().doubleValue(),
                r.renglones().stream().map(x -> new Renglon(x.productoId(), x.nombre(), x.codigoBarras(), x.piezasCompradas(), x.piezasVendidas(),
                        x.piezasRestantes(), x.totalVendido().doubleValue())).toList());
    }
}
