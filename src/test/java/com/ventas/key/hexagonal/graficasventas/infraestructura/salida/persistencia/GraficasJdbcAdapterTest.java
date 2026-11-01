package com.ventas.key.hexagonal.graficasventas.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.FormaDePagoVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasDelMes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** El SQL de verdad contra una base en memoria: criterios (devueltas fuera, rangos) y la cuenta de la comisión. */
class GraficasJdbcAdapterTest {

    private JdbcTemplate jdbc;
    private GraficasJdbcAdapter adaptador;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:graf" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE tipo_pago (id INT PRIMARY KEY, forma_pago VARCHAR(40))");
        jdbc.execute("CREATE TABLE tarifa_terminal (id INT PRIMARY KEY, tarifa DOUBLE)");
        jdbc.execute("CREATE TABLE iva_terminal (id INT PRIMARY KEY, iva DOUBLE)");
        jdbc.execute("CREATE TABLE meses_intereses (id INT PRIMARY KEY, tarifa_id INT NULL, iva_id INT NULL)");
        jdbc.execute("CREATE TABLE pagos_y_meses (id INT PRIMARY KEY, tipo_pago_id INT, meses_intereses_id INT)");
        jdbc.execute("CREATE TABLE ventas (id INT PRIMARY KEY, total_venta DOUBLE, ganancia_total DOUBLE, estado_venta VARCHAR(20), "
                + "pagos_y_meses_id INT NULL, fecha_venta TIMESTAMP)");
        jdbc.execute("CREATE TABLE gastos_surtir (id INT PRIMARY KEY, precio_gasto DOUBLE, fecha DATE)");

        jdbc.update("INSERT INTO tipo_pago VALUES (1, 'Efectivo'), (2, 'Tarjeta crédito')");
        jdbc.update("INSERT INTO tarifa_terminal VALUES (1, 0), (2, 3.5)");
        jdbc.update("INSERT INTO iva_terminal VALUES (1, 0), (2, 0.56)");
        jdbc.update("INSERT INTO meses_intereses VALUES (1, 1, 1), (2, 2, 2)");            // sin comisión / 3.5% + 0.56%
        jdbc.update("INSERT INTO pagos_y_meses VALUES (1, 1, 1), (2, 2, 2)");              // efectivo / tarjeta

        jdbc.update("INSERT INTO ventas VALUES (1, 1000, 300, 'Entregado', 1, '2026-10-05 10:00:00')");  // efectivo
        jdbc.update("INSERT INTO ventas VALUES (2, 2000, 600, 'Entregado', 2, '2026-10-31 23:30:00')");  // tarjeta, último minuto del mes
        jdbc.update("INSERT INTO ventas VALUES (3, 500, 100, 'Devuelta', 1, '2026-10-10 10:00:00')");    // devuelta: no cuenta
        jdbc.update("INSERT INTO ventas VALUES (4, 400, 80, 'Entregado', NULL, '2026-11-02 09:00:00')"); // sin forma de pago
        jdbc.update("INSERT INTO ventas VALUES (5, 700, 200, 'Entregado', 2, '2026-09-30 23:59:59')");   // septiembre
        jdbc.update("INSERT INTO ventas VALUES (6, 900, 250, 'Entregado', 1, '2026-12-01 00:00:00')");   // fuera del rango pedido

        jdbc.update("INSERT INTO gastos_surtir VALUES (1, 100, '2026-10-02'), (2, 50, '2026-10-31'), (3, 30, '2026-09-15'), (4, 999, '2026-12-01')");
        adaptador = new GraficasJdbcAdapter(jdbc);
    }

    @Test
    @DisplayName("por forma de pago: no cuenta la devuelta ni lo de fuera del rango; la venta sin forma sale aparte")
    void porFormaDePago() {
        List<FormaDePagoVentas> r = adaptador.ventasPorFormaDePago(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30));
        Map<String, FormaDePagoVentas> porNombre = r.stream().collect(java.util.stream.Collectors.toMap(FormaDePagoVentas::forma, f -> f));

        assertThat(porNombre).containsOnlyKeys("Efectivo", "Tarjeta crédito", "Sin forma de pago registrada");
        assertThat(porNombre.get("Efectivo").total()).isEqualTo(1000.0);                  // la devuelta de 500 no entra
        assertThat(porNombre.get("Efectivo").ventas()).isEqualTo(1);
        assertThat(porNombre.get("Tarjeta crédito").total()).isEqualTo(2000.0);           // la venta de las 23:30 del 31 sí entra
        assertThat(porNombre.get("Sin forma de pago registrada").total()).isEqualTo(400.0);
    }

    @Test
    @DisplayName("la comisión es total × (tarifa + IVA de la terminal) / 100; en efectivo es cero")
    void comision() {
        List<FormaDePagoVentas> r = adaptador.ventasPorFormaDePago(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30));
        FormaDePagoVentas tarjeta = r.stream().filter(f -> f.forma().equals("Tarjeta crédito")).findFirst().orElseThrow();
        FormaDePagoVentas efectivo = r.stream().filter(f -> f.forma().equals("Efectivo")).findFirst().orElseThrow();
        assertThat(tarjeta.comision()).isCloseTo(2000.0 * (3.5 + 0.56) / 100.0, within(0.001));   // 81.20
        assertThat(efectivo.comision()).isZero();
    }

    @Test
    @DisplayName("por mes: agrupa por mes, deja fuera la devuelta y lo de fuera del rango")
    void ventasPorMes() {
        Map<YearMonth, VentasDelMes> r = adaptador.ventasPorMes(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30));
        assertThat(r).containsOnlyKeys(YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11));
        assertThat(r.get(YearMonth.of(2026, 9))).isEqualTo(new VentasDelMes(1, 700.0, 200.0));
        assertThat(r.get(YearMonth.of(2026, 10))).isEqualTo(new VentasDelMes(2, 3000.0, 900.0));   // sin la devuelta
        assertThat(r.get(YearMonth.of(2026, 11))).isEqualTo(new VentasDelMes(1, 400.0, 80.0));
    }

    @Test
    @DisplayName("gastos por mes: incluye el último día del mes y deja fuera lo que no está en el rango")
    void gastosPorMes() {
        Map<YearMonth, Double> r = adaptador.gastosPorMes(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30));
        assertThat(r).containsOnlyKeys(YearMonth.of(2026, 9), YearMonth.of(2026, 10));
        assertThat(r.get(YearMonth.of(2026, 10))).isEqualTo(150.0);
        assertThat(r.get(YearMonth.of(2026, 9))).isEqualTo(30.0);
    }

    @Test
    @DisplayName("solo lee: las tablas quedan igual")
    void soloLee() {
        int antes = jdbc.queryForObject("SELECT COUNT(*) + COALESCE(SUM(total_venta), 0) FROM ventas", Integer.class);
        adaptador.ventasPorFormaDePago(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30));
        adaptador.ventasPorMes(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30));
        adaptador.gastosPorMes(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) + COALESCE(SUM(total_venta), 0) FROM ventas", Integer.class)).isEqualTo(antes);
    }
}
