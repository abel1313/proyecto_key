package com.ventas.key.hexagonal.graficasventas.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CapitalParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ClientesDelNegocio;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ComparativoDelMes;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.GananciaPorProducto;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.PeriodoDeVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.EntregasPorZona;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.MapaDeVentas;
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

    public record Celda(int diaSemana, int hora, int ventas, double total) {
    }

    public record Dia(int diaSemana, String nombre, int ventas, double total) {
    }

    public record CuandoSeVende(LocalDate desde, LocalDate hasta, int ventas, double total, Integer mejorDia, Integer mejorHora,
                                List<Dia> porDia, List<Celda> celdas) {
        public static CuandoSeVende de(MapaDeVentas m) {
            return new CuandoSeVende(m.desde(), m.hasta(), m.ventas(), m.total(), m.mejorDia(), m.mejorHora(),
                    m.porDia().stream().map(d -> new Dia(d.diaSemana(), d.nombre(), d.ventas(), d.total())).toList(),
                    m.celdas().stream().map(c -> new Celda(c.diaSemana(), c.hora(), c.ventas(), c.total())).toList());
        }
    }

    public record MesClientes(String mes, int nuevos, int recurrentes, double totalNuevos, double totalRecurrentes) {
    }

    public record MejorCliente(int clienteId, String nombre, int ventas, double total) {
    }

    public record Clientes(int meses, int clientes, int nuevos, int recurrentes, double total, List<MesClientes> porMes, List<MejorCliente> mejores) {
        public static Clientes de(ClientesDelNegocio c) {
            return new Clientes(c.meses(), c.clientes(), c.nuevos(), c.recurrentes(), c.total(),
                    c.porMes().stream().map(m -> new MesClientes(m.mes(), m.nuevos(), m.recurrentes(), m.totalNuevos(), m.totalRecurrentes())).toList(),
                    c.mejores().stream().map(m -> new MejorCliente(m.clienteId(), m.nombre(), m.ventas(), m.total())).toList());
        }
    }

    public record Zona(String zona, boolean recogerEnTienda, int pedidos, double total, double porcentaje) {
    }

    public record Entregas(LocalDate desde, LocalDate hasta, int pedidos, double total, List<Zona> zonas) {
        public static Entregas de(EntregasPorZona e) {
            return new Entregas(e.desde(), e.hasta(), e.pedidos(), e.total(),
                    e.zonas().stream().map(z -> new Zona(z.zona(), z.recogerEnTienda(), z.pedidos(), z.total(), z.porcentaje())).toList());
        }
    }

    /** Un tramo del comparativo. */
    public record Periodo(String desde, String hasta, int numeroVentas, double ventas, double ganancia, double gastos, double gananciaNeta) {
        static Periodo de(PeriodoDeVentas p) {
            return new Periodo(p.desde(), p.hasta(), p.numeroVentas(), p.ventas(), p.ganancia(), p.gastos(), p.gananciaNeta());
        }
    }

    /**
     * Cambio porcentual de este mes contra el anterior en cada número; {@code null} si el mes anterior fue cero en ese número
     * (no se puede comparar).
     */
    public record Cambios(Double numeroVentas, Double ventas, Double ganancia, Double gastos, Double gananciaNeta) {
    }

    public record Comparativo(int hastaElDia, Periodo actual, Periodo anterior, Cambios cambio) {
        public static Comparativo de(ComparativoDelMes c) {
            PeriodoDeVentas a = c.actual();
            PeriodoDeVentas b = c.anterior();
            return new Comparativo(c.hastaElDia(), Periodo.de(a), Periodo.de(b), new Cambios(
                    ComparativoDelMes.cambio(a.numeroVentas(), b.numeroVentas()), ComparativoDelMes.cambio(a.ventas(), b.ventas()),
                    ComparativoDelMes.cambio(a.ganancia(), b.ganancia()), ComparativoDelMes.cambio(a.gastos(), b.gastos()),
                    ComparativoDelMes.cambio(a.gananciaNeta(), b.gananciaNeta())));
        }
    }

    public record Rentable(int productoId, String nombre, int piezas, double vendido, double ganancia, double margen) {
    }

    public record GananciaProductos(LocalDate desde, LocalDate hasta, double gananciaTotal, double gananciaDeLosMejores,
                                    double porcentajeDeLosMejores, List<Rentable> productos) {
        public static GananciaProductos de(GananciaPorProducto g) {
            double pct = g.gananciaTotal() > 0 ? Math.round(g.gananciaDeLosMejores() * 1000.0 / g.gananciaTotal()) / 10.0 : 0.0;
            return new GananciaProductos(g.desde(), g.hasta(), g.gananciaTotal(), g.gananciaDeLosMejores(), pct,
                    g.productos().stream().map(p -> new Rentable(p.productoId(), p.nombre(), p.piezas(), p.vendido(), p.ganancia(), p.margen())).toList());
        }
    }
}
