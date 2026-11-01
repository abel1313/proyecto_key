package com.ventas.key.hexagonal.graficasventas.dominio.puerto.salida;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ArticuloParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CategoriaPedido;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.FormaDePagoVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.SaldoPendiente;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasDelMes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * De dónde salen los números de las gráficas. SOLO LECTURA. Las ventas con estado «Devuelta» no se cuentan.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface GraficasPort {

    /** Ventas entre las dos fechas (ambas incluidas), por forma de pago. */
    List<FormaDePagoVentas> ventasPorFormaDePago(LocalDate desde, LocalDate hasta);

    /** Solo los meses en que hubo ventas. */
    Map<YearMonth, VentasDelMes> ventasPorMes(LocalDate desde, LocalDate hasta);

    /** Solo los meses en que hubo gastos. */
    Map<YearMonth, Double> gastosPorMes(LocalDate desde, LocalDate hasta);

    /** Apartados e Ir pagando (estado APARTADO o FIADO, como la tarjeta «Por cobrar») con saldo mayor a un centavo. */
    List<SaldoPendiente> saldosPendientes();

    /**
     * Artículos habilitados (y de modelos habilitados y no internos) con stock, dados de alta antes de {@code corte} y sin ventas
     * desde {@code corte} (nunca vendidos incluidos).
     */
    List<ArticuloParado> articulosSinMovimiento(LocalDate corte);

    /** Pedidos por categoría: los abiertos todos, los terminados solo con fecha de pedido desde {@code desdeTerminados}. */
    Map<CategoriaPedido, Integer> pedidosPorCategoria(LocalDate desdeTerminados);

    /** Pedidos sin entregar y sin cancelar con fecha de recogida anterior a {@code hoy}. */
    int pedidosAtrasados(LocalDate hoy);
}
