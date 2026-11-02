package com.ventas.key.hexagonal.graficasventas.dominio.puerto.salida;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ArticuloParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CategoriaPedido;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ClienteVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CompraDeCliente;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ProductoRentable;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentaPorDiaYHora;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ZonaDeEntrega;
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

    /** Ventas (sin devueltas) entre las dos fechas, por fecha y hora local. */
    List<VentaPorDiaYHora> ventasPorDiaYHora(LocalDate desde, LocalDate hasta);

    /**
     * Fecha de la primera compra de cada cliente registrado en toda su historia (sin devueltas ni ventas a su propio perfil de vendedor).
     */
    Map<Integer, LocalDate> primerasCompras();

    /** Compras de clientes registrados por cliente y mes entre las dos fechas (mismos criterios que {@link #primerasCompras()}). */
    List<CompraDeCliente> comprasPorClienteYMes(LocalDate desde, LocalDate hasta);

    /** Los {@code limite} clientes que más compraron entre las dos fechas (mismos criterios), del que más al que menos. */
    List<ClienteVentas> mejoresClientes(LocalDate desde, LocalDate hasta, int limite);

    /** Pedidos no cancelados entre las dos fechas, por lugar de entrega (sin lugar = «Sin lugar de entrega»). */
    List<ZonaDeEntrega> pedidosPorZona(LocalDate desde, LocalDate hasta);

    /**
     * Ganancia por producto (modelo) de los renglones vendidos entre las dos fechas (ventas devueltas no cuentan), del que más al que
     * menos ganancia dejó, hasta {@code limite} productos.
     */
    List<ProductoRentable> gananciaPorProducto(LocalDate desde, LocalDate hasta, int limite);

    /** Ganancia de todos los renglones vendidos entre las dos fechas (ventas devueltas no cuentan). */
    double gananciaTotalDeRenglones(LocalDate desde, LocalDate hasta);
}
