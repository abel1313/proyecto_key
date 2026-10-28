package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ConfigMsiArticulo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El SQL de verdad, contra una base en memoria con las mismas dos columnas que deja
 * {@code migration_pago_tarjeta.sql}. (Los CHECK y la herencia exacta de MySQL se probaron en QA.)
 */
class AceptaMsiJdbcAdapterConBaseTest {

    private JdbcTemplate jdbc;
    private AceptaMsiJdbcAdapter adaptador;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:msi" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE producto (id INT PRIMARY KEY, acepta_msi TINYINT NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TABLE variantes (id INT PRIMARY KEY, producto_id INT NOT NULL, acepta_msi TINYINT NULL)");
        jdbc.update("INSERT INTO producto (id, acepta_msi) VALUES (1, 1), (2, 0)");
        jdbc.update("INSERT INTO variantes (id, producto_id, acepta_msi) VALUES (10, 1, NULL), (11, 1, 0), (20, 2, NULL), (21, 2, 1)");
        adaptador = new AceptaMsiJdbcAdapter(jdbc);
    }

    @Test
    void leeLaCasillaDelProducto_yDiceSiNoExiste() {
        assertThat(adaptador.productoAcepta(1)).contains(true);
        assertThat(adaptador.productoAcepta(2)).contains(false);
        assertThat(adaptador.productoAcepta(99)).isEmpty();
    }

    @Test
    void cambiarElProductoSeGuarda_yRepetirElMismoValorNoLoTomaPorInexistente() {
        assertThat(adaptador.fijarProducto(2, true)).isTrue();
        assertThat(adaptador.productoAcepta(2)).contains(true);
        assertThat(adaptador.fijarProducto(2, true)).isTrue();   // mismo valor otra vez
        assertThat(adaptador.fijarProducto(99, true)).isFalse();
    }

    @Test
    void elArticuloHeredaDelProductoOTieneLaSuya() {
        assertThat(adaptador.articulo(10)).contains(new ConfigMsiArticulo(null, true));    // hereda, producto sí
        assertThat(adaptador.articulo(11)).contains(new ConfigMsiArticulo(false, true));   // propio no, producto sí
        assertThat(adaptador.articulo(21)).contains(new ConfigMsiArticulo(true, false));   // propio sí, producto no
        assertThat(adaptador.articulo(99)).isEmpty();
    }

    @Test
    void ponerNullHaceQueElArticuloVuelvaAHeredar() {
        assertThat(adaptador.fijarArticulo(11, null)).isTrue();
        assertThat(adaptador.articulo(11).orElseThrow().hereda()).isTrue();
        assertThat(adaptador.fijarArticulo(10, true)).isTrue();
        assertThat(adaptador.articulo(10).orElseThrow().propio()).isTrue();
        assertThat(adaptador.fijarArticulo(404, true)).isFalse();
    }

    @Test
    void cambiarElProductoCambiaLoQueHeredanSusArticulos_peroNoLosQueTienenLaSuya() {
        adaptador.fijarProducto(1, false);
        assertThat(adaptador.articulo(10).orElseThrow().efectivo()).isFalse();   // heredaba: ahora no
        assertThat(adaptador.articulo(11).orElseThrow().efectivo()).isFalse();   // propia: no
        adaptador.fijarArticulo(11, true);
        assertThat(adaptador.articulo(11).orElseThrow().efectivo()).isTrue();    // propia manda aunque el producto no
    }

    @Test
    void elValorEfectivoDeVariosArticulosUsaCoalesce() {
        assertThat(adaptador.efectivos(List.of(10, 11, 20, 21, 999)))
                .containsEntry(10, true).containsEntry(11, false)
                .containsEntry(20, false).containsEntry(21, true)
                .doesNotContainKey(999);
    }
}
