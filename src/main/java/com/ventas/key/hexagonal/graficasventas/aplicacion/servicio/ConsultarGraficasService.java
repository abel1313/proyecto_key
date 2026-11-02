package com.ventas.key.hexagonal.graficasventas.aplicacion.servicio;

import com.ventas.key.hexagonal.graficasventas.dominio.excepcion.GraficasVentasException;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ArticuloParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CapitalParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ComparativoDelMes;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.GananciaPorProducto;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.PeriodoDeVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ProductoRentable;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CeldaDeCalor;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ClienteVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ClientesDelNegocio;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CompraDeCliente;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.EntregasPorZona;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.MapaDeVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.MesDeClientes;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ParteDeEntregas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.TotalDeDia;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentaPorDiaYHora;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ZonaDeEntrega;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
    public static final int CLIENTES_A_MOSTRAR = 10;
    public static final int PRODUCTOS_MAXIMO = 20;
    public static final int MESES_CLIENTES_MAXIMO = 12;
    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
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

    @Override
    public ComparativoDelMes comparativoDelMes() {
        LocalDate hoy = hoy();
        YearMonth esteMes = YearMonth.from(hoy);
        YearMonth mesAnterior = esteMes.minusMonths(1);
        int dia = hoy.getDayOfMonth();
        LocalDate finAnterior = mesAnterior.atDay(Math.min(dia, mesAnterior.lengthOfMonth()));
        return new ComparativoDelMes(dia,
                periodo(esteMes.atDay(1), hoy),
                periodo(mesAnterior.atDay(1), finAnterior));
    }

    private PeriodoDeVentas periodo(LocalDate desde, LocalDate hasta) {
        double ventas = 0, ganancia = 0;
        int numero = 0;
        for (VentasDelMes v : puerto.ventasPorMes(desde, hasta).values()) {
            numero += v.numeroVentas();
            ventas += v.ventas();
            ganancia += v.ganancia();
        }
        double gastos = puerto.gastosPorMes(desde, hasta).values().stream().mapToDouble(Double::doubleValue).sum();
        return new PeriodoDeVentas(desde.toString(), hasta.toString(), numero, redondear(ventas, 2), redondear(ganancia, 2),
                redondear(gastos, 2), redondear(ganancia - gastos, 2));
    }

    @Override
    public GananciaPorProducto gananciaPorProducto(int dias, int limite) {
        if (dias < DIAS_MINIMO || dias > DIAS_MAXIMO) {
            throw GraficasVentasException.diasInvalidos(DIAS_MINIMO, DIAS_MAXIMO);
        }
        if (limite < 1 || limite > PRODUCTOS_MAXIMO) {
            throw GraficasVentasException.limiteInvalido(1, PRODUCTOS_MAXIMO);
        }
        LocalDate hasta = hoy();
        LocalDate desde = hasta.minusDays(dias - 1L);
        List<ProductoRentable> crudos = puerto.gananciaPorProducto(desde, hasta, limite);
        List<ProductoRentable> productos = crudos.stream()
                .map(p -> new ProductoRentable(p.productoId(), p.nombre(), p.piezas(), redondear(p.vendido(), 2),
                        redondear(p.ganancia(), 2), p.vendido() > 0 ? redondear(p.ganancia() * 100.0 / p.vendido(), 1) : 0.0))
                .toList();
        double deLosMejores = productos.stream().mapToDouble(ProductoRentable::ganancia).sum();
        return new GananciaPorProducto(desde, hasta, redondear(puerto.gananciaTotalDeRenglones(desde, hasta), 2),
                redondear(deLosMejores, 2), productos);
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

    @Override
    public MapaDeVentas cuandoSeVende(int dias) {
        validarDias(dias);
        LocalDate hasta = hoy();
        LocalDate desde = hasta.minusDays(dias - 1L);
        Map<Integer, double[]> celdas = new HashMap<>();          // clave = día*100 + hora → {ventas, total}
        double[] ventasDia = new double[8];
        double[] totalDia = new double[8];
        double[] totalHora = new double[24];
        int ventas = 0;
        double total = 0;
        for (VentaPorDiaYHora v : puerto.ventasPorDiaYHora(desde, hasta)) {
            int dia = v.dia().getDayOfWeek().getValue();
            double[] c = celdas.computeIfAbsent(dia * 100 + v.hora(), k -> new double[2]);
            c[0] += v.ventas();
            c[1] += v.total();
            ventasDia[dia] += v.ventas();
            totalDia[dia] += v.total();
            totalHora[v.hora()] += v.total();
            ventas += v.ventas();
            total += v.total();
        }
        List<CeldaDeCalor> lista = new ArrayList<>();
        celdas.forEach((k, c) -> lista.add(new CeldaDeCalor(k / 100, k % 100, (int) c[0], redondear(c[1], 2))));
        lista.sort(Comparator.comparingInt(CeldaDeCalor::diaSemana).thenComparingInt(CeldaDeCalor::hora));
        List<TotalDeDia> porDia = new ArrayList<>();
        Integer mejorDia = null;
        for (int d = 1; d <= 7; d++) {
            porDia.add(new TotalDeDia(d, DIAS[d - 1], (int) ventasDia[d], redondear(totalDia[d], 2)));
            if (totalDia[d] > 0 && (mejorDia == null || totalDia[d] > totalDia[mejorDia])) mejorDia = d;
        }
        Integer mejorHora = null;
        for (int h = 0; h < 24; h++) {
            if (totalHora[h] > 0 && (mejorHora == null || totalHora[h] > totalHora[mejorHora])) mejorHora = h;
        }
        return new MapaDeVentas(desde, hasta, ventas, redondear(total, 2), lista, porDia, mejorDia, mejorHora);
    }

    @Override
    public ClientesDelNegocio clientes(int meses) {
        if (meses < MESES_MINIMO || meses > MESES_CLIENTES_MAXIMO) {
            throw GraficasVentasException.mesesInvalidos(MESES_MINIMO, MESES_CLIENTES_MAXIMO);
        }
        YearMonth ultimo = YearMonth.from(hoy());
        YearMonth primero = ultimo.minusMonths(meses - 1L);
        LocalDate desde = primero.atDay(1);
        LocalDate hasta = ultimo.atEndOfMonth();

        Map<Integer, LocalDate> primeras = puerto.primerasCompras();
        List<CompraDeCliente> compras = puerto.comprasPorClienteYMes(desde, hasta);

        Map<YearMonth, double[]> acumulado = new HashMap<>();     // {nuevos, recurrentes, totalNuevos, totalRecurrentes}
        Set<Integer> distintos = new HashSet<>();
        Set<Integer> nuevosDelPeriodo = new HashSet<>();
        double total = 0;
        for (CompraDeCliente c : compras) {
            LocalDate primera = primeras.get(c.clienteId());
            boolean esNuevo = primera == null || YearMonth.from(primera).equals(c.mes());   // sin historial = su primera compra es esta
            double[] a = acumulado.computeIfAbsent(c.mes(), k -> new double[4]);
            if (esNuevo) { a[0]++; a[2] += c.total(); nuevosDelPeriodo.add(c.clienteId()); }
            else { a[1]++; a[3] += c.total(); }
            distintos.add(c.clienteId());
            total += c.total();
        }
        List<MesDeClientes> porMes = new ArrayList<>();
        for (YearMonth mes = primero; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            double[] a = acumulado.getOrDefault(mes, new double[4]);
            porMes.add(new MesDeClientes(mes.toString(), (int) a[0], (int) a[1], redondear(a[2], 2), redondear(a[3], 2)));
        }
        List<ClienteVentas> mejores = puerto.mejoresClientes(desde, hasta, CLIENTES_A_MOSTRAR).stream()
                .map(c -> new ClienteVentas(c.clienteId(), c.nombre(), c.ventas(), redondear(c.total(), 2))).toList();
        return new ClientesDelNegocio(meses, distintos.size(), nuevosDelPeriodo.size(), redondear(total, 2), porMes, mejores);
    }

    @Override
    public EntregasPorZona entregasPorZona(int dias) {
        validarDias(dias);
        LocalDate hasta = hoy();
        LocalDate desde = hasta.minusDays(dias - 1L);
        List<ZonaDeEntrega> crudas = puerto.pedidosPorZona(desde, hasta);
        int pedidos = crudas.stream().mapToInt(ZonaDeEntrega::pedidos).sum();
        double total = crudas.stream().mapToDouble(ZonaDeEntrega::total).sum();
        List<ParteDeEntregas> partes = new ArrayList<>();
        for (ZonaDeEntrega z : crudas) {
            partes.add(new ParteDeEntregas(z.zona(), z.recogerEnTienda(), z.pedidos(), redondear(z.total(), 2),
                    pedidos > 0 ? redondear(z.pedidos() * 100.0 / pedidos, 1) : 0.0));
        }
        partes.sort(Comparator.comparingInt(ParteDeEntregas::pedidos).reversed().thenComparing(ParteDeEntregas::zona));
        return new EntregasPorZona(desde, hasta, pedidos, redondear(total, 2), partes);
    }
}
