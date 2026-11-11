package com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ventas.key.hexagonal.flete.dominio.modelo.GastoDeViaje;
import com.ventas.key.hexagonal.flete.dominio.modelo.ProductoDelViaje;
import com.ventas.key.hexagonal.flete.dominio.modelo.ViajeDeCompra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** El JSON que manda el front para crear o cambiar un viaje de compra (con TODOS sus gastos y productos). */
public record ViajeRequest(String nombre,
                           @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fecha,
                           BigDecimal fleteTotal, String nota, List<Gasto> gastos, List<Producto> productos) {

    public record Gasto(String concepto, BigDecimal monto, Boolean cuentaParaCubrir) {
    }

    public record Producto(Integer productoId, Integer piezas, BigDecimal costoUnitario, BigDecimal fletePorPieza) {
    }

    public ViajeDeCompra aDominio() {
        List<GastoDeViaje> g = gastos == null ? List.of() : gastos.stream()
                .map(x -> new GastoDeViaje(x.concepto(), x.monto(), x.cuentaParaCubrir() == null || x.cuentaParaCubrir())).toList();
        List<ProductoDelViaje> p = productos == null ? List.of() : productos.stream()
                .map(x -> new ProductoDelViaje(x.productoId() == null ? 0 : x.productoId(), x.piezas() == null ? 0 : x.piezas(),
                        x.costoUnitario(), x.fletePorPieza())).toList();
        return new ViajeDeCompra(null, nombre, fecha, fleteTotal, nota, g, p);
    }
}
