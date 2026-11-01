package com.ventas.key.hexagonal.graficasventas.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CapitalParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.DineroPorCobrar;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.EstadoDePedidos;
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

    public record Tramo(String etiqueta, int pedidos, double saldo) {
    }

    public record CasoCobro(int pedidoId, String cliente, String tipo, double saldo, long dias) {
    }

    public record PorCobrar(double total, int pedidos, List<Tramo> tramos, List<CasoCobro> masAntiguos) {
        public static PorCobrar de(DineroPorCobrar d) {
            return new PorCobrar(d.total(), d.pedidos(),
                    d.tramos().stream().map(t -> new Tramo(t.etiqueta(), t.pedidos(), t.saldo())).toList(),
                    d.masAntiguos().stream().map(c -> new CasoCobro(c.pedidoId(), c.cliente(), c.tipo(), c.saldo(), c.dias())).toList());
        }
    }

    public record ArticuloQuieto(int articuloId, String nombre, int stock, double costo, double capital, LocalDate ultimaVenta, Long diasSinVender) {
    }

    public record Parado(int dias, int articulos, int piezas, double capital, List<ArticuloQuieto> mayores) {
        public static Parado de(CapitalParado c) {
            return new Parado(c.dias(), c.articulos(), c.piezas(), c.capital(),
                    c.mayores().stream().map(a -> new ArticuloQuieto(a.articuloId(), a.nombre(), a.stock(), a.costo(), a.capital(),
                            a.ultimaVenta(), a.diasSinVender())).toList());
        }
    }

    public record Conteo(String codigo, String etiqueta, boolean activo, int pedidos) {
    }

    public record PedidosPorEstado(LocalDate desde, LocalDate hasta, int activos, int atrasados, List<Conteo> estados) {
        public static PedidosPorEstado de(EstadoDePedidos e) {
            return new PedidosPorEstado(e.desde(), e.hasta(), e.activos(), e.atrasados(),
                    e.estados().stream().map(c -> new Conteo(c.codigo(), c.etiqueta(), c.activo(), c.pedidos())).toList());
        }
    }
}
