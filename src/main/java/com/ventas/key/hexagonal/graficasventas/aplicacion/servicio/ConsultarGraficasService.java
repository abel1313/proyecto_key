package com.ventas.key.hexagonal.graficasventas.aplicacion.servicio;

import com.ventas.key.hexagonal.graficasventas.dominio.excepcion.GraficasVentasException;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ArticuloParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CapitalParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CasoCapitalParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CasoPorCobrar;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.DineroPorCobrar;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.EstadoDePedidos;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.FormaDePagoVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.SaldoPendiente;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.TramoPorCobrar;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ParteDelTotal;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.PuntoMensual;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasDelMes;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasPorFormaDePago;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.entrada.ConsultarGraficasCasoUso;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.salida.GraficasPort;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * [Hexagonal: Use Case] [Clean: Use Cases] Java puro, sin Spring.
 *
 * <p>Pone las fechas por defecto, calcula porcentajes y rellena con ceros los meses sin movimiento para que la gráfica
 * nunca tenga huecos.
 */
public class ConsultarGraficasService implements ConsultarGraficasCasoUso {

    public static final int MESES_MINIMO = 1;
    public static final int MESES_MAXIMO = 24;
    public static final int DIAS_MINIMO = 7;
    public static final int DIAS_MAXIMO = 365;
    /** Cuántos casos concretos se muestran en las listas de «a quién cobrarle» y «qué está parado». */
    public static final int CASOS_A_MOSTRAR = 8;
    public static final int ARTICULOS_PARADOS_A_MOSTRAR = 10;
    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private final GraficasPort puerto;
    private final Clock reloj;

    public ConsultarGraficasService(GraficasPort puerto, Clock reloj) {
        this.puerto = puerto;
        this.reloj = reloj;
    }

    @Override
    public VentasPorFormaDePago ventasPorFormaDePago(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : hoy();
        LocalDate inicio = desde != null ? desde : fin.withDayOfMonth(1);
        if (inicio.isAfter(fin)) {
            throw GraficasVentasException.fechasInvalidas();
        }
        List<FormaDePagoVentas> crudas = puerto.ventasPorFormaDePago(inicio, fin);
        double total = crudas.stream().mapToDouble(FormaDePagoVentas::total).sum();
        List<ParteDelTotal> partes = new ArrayList<>();
        for (FormaDePagoVentas f : crudas) {
            double porcentaje = total > 0 ? redondear(f.total() * 100.0 / total, 1) : 0.0;
            partes.add(new ParteDelTotal(f.forma(), f.ventas(), redondear(f.total(), 2), porcentaje, redondear(f.comision(), 2)));
        }
        partes.sort(Comparator.comparingDouble(ParteDelTotal::total).reversed());
        double comisionTotal = crudas.stream().mapToDouble(FormaDePagoVentas::comision).sum();
        return new VentasPorFormaDePago(inicio, fin, redondear(total, 2), redondear(comisionTotal, 2), partes);
    }

    @Override
    public List<PuntoMensual> ventasYGastosPorMes(int meses) {
        if (meses < MESES_MINIMO || meses > MESES_MAXIMO) {
            throw GraficasVentasException.mesesInvalidos(MESES_MINIMO, MESES_MAXIMO);
        }
        YearMonth ultimo = YearMonth.from(hoy());
        YearMonth primero = ultimo.minusMonths(meses - 1L);
        LocalDate desde = primero.atDay(1);
        LocalDate hasta = ultimo.atEndOfMonth();

        Map<YearMonth, VentasDelMes> ventas = puerto.ventasPorMes(desde, hasta);
        Map<YearMonth, Double> gastos = puerto.gastosPorMes(desde, hasta);

        List<PuntoMensual> serie = new ArrayList<>();
        for (YearMonth mes = primero; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            VentasDelMes v = ventas.getOrDefault(mes, new VentasDelMes(0, 0.0, 0.0));
            double g = gastos.getOrDefault(mes, 0.0);
            serie.add(new PuntoMensual(mes.toString(), v.numeroVentas(), redondear(v.ventas(), 2), redondear(v.ganancia(), 2),
                    redondear(g, 2), redondear(v.ganancia() - g, 2)));
        }
        return serie;
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj.withZone(ZONA));
    }

    private static double redondear(double valor, int decimales) {
        double f = Math.pow(10, decimales);
        return Math.round(valor * f) / f;
    }

    @Override
    public DineroPorCobrar dineroPorCobrar() {
        LocalDate hoy = hoy();
        List<SaldoPendiente> saldos = puerto.saldosPendientes();
        double[] sumas = new double[3];
        int[] cuentas = new int[3];
        List<CasoPorCobrar> casos = new ArrayList<>();
        for (SaldoPendiente s : saldos) {
            long dias = Math.max(0, ChronoUnit.DAYS.between(s.fechaPedido(), hoy));
            int tramo = dias <= 7 ? 0 : dias <= 30 ? 1 : 2;
            sumas[tramo] += s.saldo();
            cuentas[tramo]++;
            casos.add(new CasoPorCobrar(s.pedidoId(), s.cliente(), "APARTADO".equals(s.tipo()) ? "Apartado" : "Ir pagando",
                    redondear(s.saldo(), 2), dias));
        }
        casos.sort(Comparator.comparingLong(CasoPorCobrar::dias).reversed().thenComparing(Comparator.comparingDouble(CasoPorCobrar::saldo).reversed()));
        List<TramoPorCobrar> tramos = List.of(
                new TramoPorCobrar("0 a 7 días", cuentas[0], redondear(sumas[0], 2)),
                new TramoPorCobrar("8 a 30 días", cuentas[1], redondear(sumas[1], 2)),
                new TramoPorCobrar("Más de 30 días", cuentas[2], redondear(sumas[2], 2)));
        double total = sumas[0] + sumas[1] + sumas[2];
        return new DineroPorCobrar(redondear(total, 2), saldos.size(), tramos, casos.stream().limit(CASOS_A_MOSTRAR).toList());
    }

    @Override
    public CapitalParado capitalParado(int dias) {
        validarDias(dias);
        LocalDate hoy = hoy();
        List<ArticuloParado> parados = puerto.articulosSinMovimiento(hoy.minusDays(dias));
        List<CasoCapitalParado> casos = new ArrayList<>();
        int piezas = 0;
        double capital = 0;
        for (ArticuloParado a : parados) {
            double valor = a.stock() * a.costo();
            piezas += a.stock();
            capital += valor;
            Long sinVender = a.ultimaVenta() == null ? null : ChronoUnit.DAYS.between(a.ultimaVenta(), hoy);
            casos.add(new CasoCapitalParado(a.articuloId(), a.nombre(), a.stock(), redondear(a.costo(), 2), redondear(valor, 2),
                    a.ultimaVenta(), sinVender));
        }
        casos.sort(Comparator.comparingDouble(CasoCapitalParado::capital).reversed());
        return new CapitalParado(dias, parados.size(), piezas, redondear(capital, 2),
                casos.stream().limit(ARTICULOS_PARADOS_A_MOSTRAR).toList());
    }

    @Override
    public EstadoDePedidos pedidosPorEstado(int dias) {
        validarDias(dias);
        LocalDate hoy = hoy();
        LocalDate desde = hoy.minusDays(dias);
        return EstadoDePedidos.de(desde, hoy, puerto.pedidosPorCategoria(desde), puerto.pedidosAtrasados(hoy));
    }

    private static void validarDias(int dias) {
        if (dias < DIAS_MINIMO || dias > DIAS_MAXIMO) {
            throw GraficasVentasException.diasInvalidos(DIAS_MINIMO, DIAS_MAXIMO);
        }
    }
}
