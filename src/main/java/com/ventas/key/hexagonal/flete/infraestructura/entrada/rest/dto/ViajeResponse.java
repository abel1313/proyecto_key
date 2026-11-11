package com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ventas.key.hexagonal.flete.dominio.modelo.ViajeDeCompra;

import java.time.LocalDate;
import java.util.List;

/**
 * Un viaje de compra para el front. Trae lo ya calculado: {@code costoACubrir} (flete + gastos que cuentan), {@code fleteAsignado} (lo que ya llevan las
 * piezas), {@code faltaAsignar}, {@code excedente} (flete de más en las piezas = ganancia), {@code compraTotal}, {@code piezasTotal} y
 * {@code fleteSugeridoPorPieza} (costo a cubrir ÷ piezas).
 */
public record ViajeResponse(Integer id, String nombre, @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fecha, double fleteTotal, String nota,
                            List<Gasto> gastos, List<Producto> productos,
                            double gastosTotal, double costoACubrir, double fleteAsignado, double faltaAsignar, double excedente,
                            double compraTotal, int piezasTotal, double fleteSugeridoPorPieza) {

    public record Gasto(String concepto, double monto, boolean cuentaParaCubrir) {
    }

    public record Producto(int productoId, String nombre, String codigoBarras, int piezas, double costoUnitario, double fletePorPieza, double fleteDelRenglon) {
    }

    public static ViajeResponse de(ViajeDeCompra v) {
        return new ViajeResponse(v.id(), v.nombre(), v.fecha(), v.fleteTotal().doubleValue(), v.nota(),
                v.gastos().stream().map(g -> new Gasto(g.concepto(), g.monto().doubleValue(), g.cuentaParaCubrir())).toList(),
                v.productos().stream().map(p -> new Producto(p.productoId(), p.nombre(), p.codigoBarras(), p.piezas(), p.costoUnitario().doubleValue(),
                        p.fletePorPieza().doubleValue(), p.fleteDelRenglon().doubleValue())).toList(),
                v.gastosTotal().doubleValue(), v.costoACubrir().doubleValue(), v.fleteAsignado().doubleValue(), v.faltaAsignar().doubleValue(),
                v.excedente().doubleValue(), v.compraTotal().doubleValue(), v.piezasTotal(), v.fleteSugeridoPorPieza().doubleValue());
    }
}
