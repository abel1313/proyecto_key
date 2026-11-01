package com.ventas.key.hexagonal.graficasventas.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ArticuloParado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** El SQL de «capital parado»: qué artículos cuentan y cuáles no, contra una base en memoria. */
class CapitalParadoJdbcTest {

    private static final LocalDate CORTE = LocalDate.of(2026, 9, 2);   // «hoy» 2026-11-01 menos 60 días

    private JdbcTemplate jdbc;
    private GraficasJdbcAdapter adaptador;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:cap" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE producto (id INT PRIMARY KEY, nombre VARCHAR(80), precio_costo DOUBLE, habilitado CHAR(1), es_catalogo_interno TINYINT)");
        jdbc.execute("CREATE TABLE variantes (id INT PRIMARY KEY, producto_id INT, stock INT, habilitado CHAR(1), talla VARCHAR(10), "
                + "color VARCHAR(30), fecha_creacion TIMESTAMP NULL)");
        jdbc.execute("CREATE TABLE detalle_venta_variantes (id INT PRIMARY KEY, variante_id INT, fecha_venta DATE)");

        jdbc.update("INSERT INTO producto VALUES (1, 'Pantalón', 100, '1', 0), (2, 'Interno', 10, '1', 1), (3, 'Apagado', 10, '0', 0), (4, 'Bolsa', 50, '1', 0)");
        jdbc.update("INSERT INTO variantes VALUES (10, 1, 3, '1', 'M', 'Rojo', '2026-01-01 10:00:00')");    // vendió en junio: PARADO
        jdbc.update("INSERT INTO variantes VALUES (11, 1, 2, '1', 'L', 'Azul', '2026-01-01 10:00:00')");    // vendió hace 2 semanas: no
        jdbc.update("INSERT INTO variantes VALUES (12, 1, 5, '1', 'S', 'Negro', '2026-10-20 10:00:00')");   // muy nuevo: no
        jdbc.update("INSERT INTO variantes VALUES (13, 1, 0, '1', 'XL', 'Gris', '2026-01-01 10:00:00')");   // sin stock: no
        jdbc.update("INSERT INTO variantes VALUES (14, 1, 4, '0', 'XS', 'Gris', '2026-01-01 10:00:00')");   // apagado: no
        jdbc.update("INSERT INTO variantes VALUES (15, 2, 9, '1', 'U', 'Negro', '2026-01-01 10:00:00')");   // modelo interno: no
        jdbc.update("INSERT INTO variantes VALUES (16, 3, 9, '1', 'U', 'Negro', '2026-01-01 10:00:00')");   // modelo apagado: no
        jdbc.update("INSERT INTO variantes VALUES (17, 4, 10, '1', 'U', 'Café', '2026-02-01 10:00:00')");   // nunca vendido: PARADO
        jdbc.update("INSERT INTO variantes VALUES (18, 1, 1, '1', 'M', 'Verde', NULL)");                    // sin fecha de alta, nunca vendido: PARADO
        jdbc.update("INSERT INTO detalle_venta_variantes VALUES (1, 10, '2026-06-01'), (2, 10, '2026-03-01'), (3, 11, '2026-10-15')");
        adaptador = new GraficasJdbcAdapter(jdbc);
    }

    @Test
    @DisplayName("parados: con stock, habilitados, no internos, con más de 60 días en el catálogo y sin ventas en 60 días (nunca vendidos incluidos)")
    void parados() {
        List<ArticuloParado> r = adaptador.articulosSinMovimiento(CORTE);
        assertThat(r).extracting(ArticuloParado::articuloId).containsExactlyInAnyOrder(10, 17, 18);
    }

    @Test
    @DisplayName("trae el nombre completo, el stock, el costo y la ÚLTIMA venta (la más reciente, no la primera)")
    void datosDelArticulo() {
        ArticuloParado pantalon = adaptador.articulosSinMovimiento(CORTE).stream().filter(a -> a.articuloId() == 10).findFirst().orElseThrow();
        assertThat(pantalon.nombre()).isEqualTo("Pantalón talla M Rojo");
        assertThat(pantalon.stock()).isEqualTo(3);
        assertThat(pantalon.costo()).isEqualTo(100.0);
        assertThat(pantalon.ultimaVenta()).isEqualTo(LocalDate.of(2026, 6, 1));
        ArticuloParado bolsa = adaptador.articulosSinMovimiento(CORTE).stream().filter(a -> a.articuloId() == 17).findFirst().orElseThrow();
        assertThat(bolsa.ultimaVenta()).isNull();
    }

    @Test
    @DisplayName("una venta justo en la fecha de corte ya cuenta como movimiento")
    void ventaEnElCorte() {
        jdbc.update("INSERT INTO detalle_venta_variantes VALUES (9, 17, '2026-09-02')");
        assertThat(adaptador.articulosSinMovimiento(CORTE)).extracting(ArticuloParado::articuloId).doesNotContain(17);
    }
}
