package com.ventas.key.hexagonal.graficasventas.aplicacion.servicio;

import com.ventas.key.hexagonal.graficasventas.dominio.excepcion.GraficasVentasException;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.FormaDePagoVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ParteDelTotal;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.PuntoMensual;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasDelMes;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasPorFormaDePago;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.entrada.ConsultarGraficasCasoUso;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.salida.GraficasPort;

import java.time.Clock;
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
}
