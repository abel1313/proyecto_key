package com.ventas.key.hexagonal.graficasventas.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ClienteVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CompraDeCliente;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentaPorDiaYHora;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ZonaDeEntrega;
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

/** El SQL de «cuándo se vende», «clientes» y «entregas por zona» contra una base en memoria. */
class ClientesZonasYHorasJdbcTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_31 = LocalDate.of(2026, 10, 31);

    private JdbcTemplate jdbc;
    private GraficasJdbcAdapter adaptador;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:cz" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE clientes (id INT PRIMARY KEY, nombre_persona VARCHAR(60), apeido_paterno VARCHAR(60), usuario_id INT NULL)");
        jdbc.execute("CREATE TABLE ventas (id INT PRIMARY KEY, cliente_id INT NULL, usuario_id INT, total_venta DOUBLE, estado_venta VARCHAR(20), fecha_venta TIMESTAMP)");
        jdbc.execute("CREATE TABLE lugares_entrega (id INT PRIMARY KEY, nombre VARCHAR(80), es_recoger_en_tienda BOOLEAN NULL)");
        jdbc.execute("CREATE TABLE pedidos (id INT PRIMARY KEY, estado_pedido VARCHAR(30), total_pedido DOUBLE, fecha_pedido DATE, lugar_entrega_id INT NULL)");

        jdbc.update("INSERT INTO clientes VALUES (1, 'Ana', 'López', NULL), (2, 'Luis', 'Pérez', NULL), (3, 'Admin', 'Mostrador', 3)");   // el 3 es el perfil del vendedor 3
        // 2026-10-05 es lunes; 2026-10-07 es miércoles
        jdbc.update("INSERT INTO ventas VALUES (1, 1, 99, 100, 'Entregado', '2026-10-05 10:15:00')");
        jdbc.update("INSERT INTO ventas VALUES (2, 1, 99, 200, 'Entregado', '2026-10-05 10:45:00')");
        jdbc.update("INSERT INTO ventas VALUES (3, 2, 99, 500, 'Entregado', '2026-10-07 18:30:00')");
        jdbc.update("INSERT INTO ventas VALUES (4, 3, 3, 50, 'Entregado', '2026-10-07 18:05:00')");       // mostrador: cuenta como venta, NO como cliente
        jdbc.update("INSERT INTO ventas VALUES (5, 2, 99, 999, 'Devuelta', '2026-10-08 11:00:00')");      // devuelta: no cuenta en nada
        jdbc.update("INSERT INTO ventas VALUES (6, 1, 99, 300, 'Entregado', '2026-09-10 12:00:00')");     // septiembre
        jdbc.update("INSERT INTO ventas VALUES (7, 2, 99, 80, 'Entregado', '2026-08-01 12:00:00')");      // la primera compra de Luis
        jdbc.update("INSERT INTO ventas VALUES (8, NULL, 99, 70, 'Entregado', '2026-10-09 09:00:00')");   // sin cliente registrado

        jdbc.update("INSERT INTO lugares_entrega VALUES (1, 'Centro', FALSE), (2, 'Recoger en tienda', TRUE)");
        jdbc.update("INSERT INTO pedidos VALUES (1, 'Entregado', 100, '2026-10-02', 1)");
        jdbc.update("INSERT INTO pedidos VALUES (2, 'Pendiente', 200, '2026-10-03', 1)");
        jdbc.update("INSERT INTO pedidos VALUES (3, 'Pendiente', 50, '2026-10-04', 2)");
        jdbc.update("INSERT INTO pedidos VALUES (4, 'CANCELADO', 999, '2026-10-04', 1)");                 // cancelado: no cuenta
        jdbc.update("INSERT INTO pedidos VALUES (5, 'Pendiente', 70, '2026-10-05', NULL)");               // sin lugar de entrega
        jdbc.update("INSERT INTO pedidos VALUES (6, 'Pendiente', 10, '2026-08-01', 1)");                  // fuera del periodo
        adaptador = new GraficasJdbcAdapter(jdbc);
    }

    @Test
    @DisplayName("por día y hora: agrupa por fecha y hora, cuenta la venta de mostrador y la sin cliente, y deja fuera la devuelta")
    void porDiaYHora() {
        List<VentaPorDiaYHora> r = adaptador.ventasPorDiaYHora(OCT_1, OCT_31);

        assertThat(r).hasSize(3);
        assertThat(r).filteredOn(v -> v.dia().equals(LocalDate.of(2026, 10, 5)) && v.hora() == 10).singleElement()
                .satisfies(v -> { assertThat(v.ventas()).isEqualTo(2); assertThat(v.total()).isEqualTo(300.0); });
        assertThat(r).filteredOn(v -> v.dia().equals(LocalDate.of(2026, 10, 7)) && v.hora() == 18).singleElement()
                .satisfies(v -> { assertThat(v.ventas()).isEqualTo(2); assertThat(v.total()).isEqualTo(550.0); });   // 500 + los 50 de mostrador
        assertThat(r).filteredOn(v -> v.hora() == 9).singleElement().satisfies(v -> assertThat(v.total()).isEqualTo(70.0));
        assertThat(r).extracting(VentaPorDiaYHora::total).doesNotContain(999.0);
    }

    @Test
    @DisplayName("primeras compras: la más antigua de cada cliente; sin devueltas, sin mostrador y sin ventas sin cliente")
    void primerasCompras() {
        Map<Integer, LocalDate> r = adaptador.primerasCompras();
        assertThat(r).containsOnlyKeys(1, 2);
        assertThat(r.get(1)).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(r.get(2)).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    @DisplayName("compras por cliente y mes: solo clientes reales, sin la devuelta ni el mostrador")
    void comprasPorClienteYMes() {
        List<CompraDeCliente> r = adaptador.comprasPorClienteYMes(LocalDate.of(2026, 9, 1), OCT_31);
        assertThat(r).extracting(c -> c.clienteId() + "@" + c.mes()).containsExactlyInAnyOrder("1@2026-09", "1@2026-10", "2@2026-10");
        assertThat(r).filteredOn(c -> c.clienteId() == 1 && c.mes().equals(YearMonth.of(2026, 10))).singleElement()
                .satisfies(c -> { assertThat(c.ventas()).isEqualTo(2); assertThat(c.total()).isEqualTo(300.0); });
        assertThat(r).filteredOn(c -> c.clienteId() == 2).singleElement().satisfies(c -> assertThat(c.total()).isEqualTo(500.0)); // sin los 999 devueltos
    }

    @Test
    @DisplayName("mejores clientes: del que más compró al que menos, con su nombre, y respeta el límite")
    void mejoresClientes() {
        List<ClienteVentas> r = adaptador.mejoresClientes(OCT_1, OCT_31, 10);
        assertThat(r).extracting(ClienteVentas::nombre).containsExactly("Luis Pérez", "Ana López");
        assertThat(r.get(0).total()).isEqualTo(500.0);
        assertThat(adaptador.mejoresClientes(OCT_1, OCT_31, 1)).hasSize(1);
    }

    @Test
    @DisplayName("por zona: agrupa por lugar de entrega; sin cancelados, sin lo de fuera del periodo; sin lugar sale aparte")
    void porZona() {
        List<ZonaDeEntrega> r = adaptador.pedidosPorZona(OCT_1, OCT_31);
        assertThat(r).extracting(ZonaDeEntrega::zona).containsExactlyInAnyOrder("Centro", "Recoger en tienda", "Sin lugar de entrega");
        assertThat(r).filteredOn(z -> z.zona().equals("Centro")).singleElement().satisfies(z -> {
            assertThat(z.pedidos()).isEqualTo(2);
            assertThat(z.total()).isEqualTo(300.0);
            assertThat(z.recogerEnTienda()).isFalse();
        });
        assertThat(r).filteredOn(z -> z.zona().equals("Recoger en tienda")).singleElement().satisfies(z -> assertThat(z.recogerEnTienda()).isTrue());
        assertThat(r).filteredOn(z -> z.zona().equals("Sin lugar de entrega")).singleElement().satisfies(z -> assertThat(z.total()).isEqualTo(70.0));
    }
}
