package com.ventas.key.hexagonal.datosprueba;

import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.PlanInvalidoException;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ArticuloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.CatalogoAleatorio;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ModeloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.PlanDeDatos;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.TipoPedidoPrueba;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas del dominio datosprueba que no necesitan base de datos (README: R4, R6, R9, R11). */
class DatosPruebaDominioTest {

    @Test
    void r11_los_topes_de_una_corrida() {
        new PlanDeDatos(20_000, 1, 4, 3_000, 1L);
        assertThatThrownBy(() -> new PlanDeDatos(20_001, 1, 4, 0, 1L)).isInstanceOf(PlanInvalidoException.class);
        assertThatThrownBy(() -> new PlanDeDatos(0, 1, 4, 0, 1L)).isInstanceOf(PlanInvalidoException.class);
        assertThatThrownBy(() -> new PlanDeDatos(10, 0, 4, 0, 1L)).isInstanceOf(PlanInvalidoException.class);
        assertThatThrownBy(() -> new PlanDeDatos(10, 1, 5, 0, 1L)).isInstanceOf(PlanInvalidoException.class);
        assertThatThrownBy(() -> new PlanDeDatos(10, 3, 2, 0, 1L)).isInstanceOf(PlanInvalidoException.class);
        assertThatThrownBy(() -> new PlanDeDatos(10, 1, 4, 3_001, 1L)).isInstanceOf(PlanInvalidoException.class);
    }

    @Test
    void r4_codigo_de_prueba_2098_y_13_digitos() {
        assertThat(CatalogoAleatorio.codigoBarras(1)).isEqualTo("2098000000001");
        assertThat(CatalogoAleatorio.codigoBarras(20_000)).isEqualTo("2098000020000").hasSize(13);
    }

    @Test
    void misma_semilla_mismo_catalogo() {
        ModeloDePrueba a = CatalogoAleatorio.modelo(7, 1, 4, new Random(42));
        ModeloDePrueba b = CatalogoAleatorio.modelo(7, 1, 4, new Random(42));
        assertThat(a).isEqualTo(b);
    }

    @Test
    void r6_y_los_modelos_parecen_de_la_tienda() {
        Random rnd = new Random(123);
        for (int n = 1; n <= 5_000; n++) {
            ModeloDePrueba m = CatalogoAleatorio.modelo(n, 1, 4, rnd);

            assertThat(m.articulos()).hasSizeBetween(1, 4);
            Set<String> distintos = new HashSet<>();
            for (ArticuloDePrueba art : m.articulos()) {
                assertThat(art.stock()).isBetween(CatalogoAleatorio.STOCK_MIN, CatalogoAleatorio.STOCK_MAX);
                assertThat(distintos.add(art.talla() + "|" + art.color()))
                        .as("dos articulos iguales en %s", m.nombre()).isTrue();
            }
            // R6: el stock del modelo es la suma de sus articulos.
            assertThat(m.stock()).isEqualTo(m.articulos().stream().mapToInt(ArticuloDePrueba::stock).sum());
            assertThat(m.stock()).isGreaterThan(5);

            assertThat(m.precioVenta() % 10).isEqualTo(9);
            assertThat(m.precioRebaja()).isPositive().isLessThanOrEqualTo(m.precioVenta());
            assertThat(m.precioCosto()).isPositive().isLessThan(m.precioVenta());
            assertThat(m.nombre()).endsWith("QA-" + n);
            assertThat(CatalogoAleatorio.categorias()).contains(m.categoria());
        }
    }

    @Test
    void r9_la_mezcla_de_pedidos() {
        Random rnd = new Random(9);
        Map<TipoPedidoPrueba, Integer> cuenta = new EnumMap<>(TipoPedidoPrueba.class);
        for (int i = 0; i < 100_000; i++) {
            cuenta.merge(TipoPedidoPrueba.elegir(rnd), 1, Integer::sum);
        }
        assertThat(cuenta.get(TipoPedidoPrueba.CONTADO)).isBetween(39_000, 41_000);
        assertThat(cuenta.get(TipoPedidoPrueba.APARTADO)).isBetween(19_000, 21_000);
        assertThat(cuenta.get(TipoPedidoPrueba.APARTADO_PAGADO)).isBetween(9_000, 11_000);
        assertThat(cuenta.get(TipoPedidoPrueba.IR_PAGANDO)).isBetween(29_000, 31_000);

        assertThat(TipoPedidoPrueba.CONTADO.tipoEnVenta()).isEqualTo("NORMAL");
        assertThat(TipoPedidoPrueba.APARTADO_PAGADO.tipoEnVenta()).isEqualTo("APARTADO");
        assertThat(TipoPedidoPrueba.IR_PAGANDO.tipoEnVenta()).isEqualTo("FIADO");
    }
}
