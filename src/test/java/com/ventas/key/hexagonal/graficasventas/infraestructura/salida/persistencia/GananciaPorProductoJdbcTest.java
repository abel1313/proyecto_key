package com.ventas.key.hexagonal.graficasventas.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ProductoRentable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Ganancia por producto contra una base en memoria: SÍ cuentan los renglones del periodo; NO las devueltas ni las de otras fechas. */
class GananciaPorProductoJdbcTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

    private GraficasJdbcAdapter adaptador;

    @BeforeEach
    void preparar() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:gan" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE producto (id INT PRIMARY KEY, nombre VARCHAR(80))");
        jdbc.execute("CREATE TABLE variantes (id INT PRIMARY KEY, producto_id INT)");
        jdbc.execute("CREATE TABLE ventas (id INT PRIMARY KEY, estado_venta VARCHAR(20))");
        jdbc.execute("CREATE TABLE detalle_venta_variantes (id INT PRIMARY KEY, venta_id INT, variante_id INT, cantidad INT, sub_total DOUBLE, "
                + "ganancia DOUBLE, fecha_venta DATE)");
        jdbc.update("INSERT INTO producto VALUES (1, 'BOLSA LUCIA'), (2, 'FALDA'), (3, 'PANTALON')");
        jdbc.update("INSERT INTO variantes VALUES (10, 1), (11, 1), (20, 2), (30, 3)");
        jdbc.update("INSERT INTO ventas VALUES (1, 'Completada'), (2, 'Devuelta')");
        // Bolsa (dos artículos): 300 + 400 de ganancia = 700
        jdbc.update("INSERT INTO detalle_venta_variantes VALUES (1, 1, 10, 2, 700, 300, '2026-10-05')");
        jdbc.update("INSERT INTO detalle_venta_variantes VALUES (2, 1, 11, 2, 700, 400, '2026-10-20')");
        // Falda: 150
        jdbc.update("INSERT INTO detalle_venta_variantes VALUES (3, 1, 20, 2, 600, 150, '2026-10-10')");
        // Pantalón: venta devuelta (no cuenta) y otra fuera de fechas (no cuenta)
        jdbc.update("INSERT INTO detalle_venta_variantes VALUES (4, 2, 30, 1, 500, 250, '2026-10-12')");
        jdbc.update("INSERT INTO detalle_venta_variantes VALUES (5, 1, 30, 1, 500, 250, '2026-09-12')");
        adaptador = new GraficasJdbcAdapter(jdbc);
    }

    @Test
    @DisplayName("suma por producto (no por artículo), de mayor a menor, sin devueltas ni fechas de fuera")
    void gananciaPorProducto() {
        List<ProductoRentable> r = adaptador.gananciaPorProducto(DESDE, HASTA, 10);

        assertThat(r).extracting(ProductoRentable::nombre).containsExactly("BOLSA LUCIA", "FALDA");
        assertThat(r.get(0).ganancia()).isEqualTo(700.0);
        assertThat(r.get(0).piezas()).isEqualTo(4);
        assertThat(r.get(1).ganancia()).isEqualTo(150.0);
    }

    @Test
    @DisplayName("el límite recorta la lista y el total sigue siendo el de todos")
    void limiteYTotal() {
        assertThat(adaptador.gananciaPorProducto(DESDE, HASTA, 1)).extracting(ProductoRentable::nombre).containsExactly("BOLSA LUCIA");
        assertThat(adaptador.gananciaTotalDeRenglones(DESDE, HASTA)).isEqualTo(850.0);
    }
}
