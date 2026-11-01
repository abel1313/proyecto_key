package com.ventas.key.hexagonal.graficasventas.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.PuntoMensual;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasPorFormaDePago;

import java.time.LocalDate;
import java.util.List;

/** El JSON de las gráficas del Dashboard. Lo que el front necesita, sin datos internos. */
public final class GraficasResponse {

    private GraficasResponse() {
    }

    public record FormaDePago(String forma, int ventas, double total, double porcentaje, double comision) {
    }

    public record PorFormaDePago(LocalDate desde, LocalDate hasta, double total, double comisionTotal, List<FormaDePago> formas) {
        public static PorFormaDePago de(VentasPorFormaDePago v) {
            return new PorFormaDePago(v.desde(), v.hasta(), v.total(), v.comisionTotal(),
                    v.formas().stream().map(f -> new FormaDePago(f.forma(), f.ventas(), f.total(), f.porcentaje(), f.comision())).toList());
        }
    }

    public record Mes(String mes, int numeroVentas, double ventas, double ganancia, double gastos, double gananciaNeta) {
        public static Mes de(PuntoMensual p) {
            return new Mes(p.mes(), p.numeroVentas(), p.ventas(), p.ganancia(), p.gastos(), p.gananciaNeta());
        }
    }
}
