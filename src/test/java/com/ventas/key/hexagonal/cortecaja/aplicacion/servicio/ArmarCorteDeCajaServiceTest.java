package com.ventas.key.hexagonal.cortecaja.aplicacion.servicio;

import com.ventas.key.hexagonal.cortecaja.dominio.modelo.CorteDeCaja;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.PedidoPorCobrar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corte de caja (dueño, 2026-11-04): los pedidos que NO están pagados ni entregados, por zona de entrega, con lo que falta por cobrar de cada
 * uno, la suma por zona y el total general. Java puro: sin Spring ni base.
 */
class ArmarCorteDeCajaServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-04T18:00:00Z"), ZoneOffset.UTC);
    private static final int LUVIANOS = 1;
    private static final int TEJUPILCO = 2;

    private static PedidoPorCobrar pedido(int id, String total, String pagado, Integer zonaId, String zona) {
        return new PedidoPorCobrar(id, "Cliente " + id, 1, new BigDecimal(total), new BigDecimal(pagado),
                new BigDecimal(pagado).signum() > 0 ? 1 : 0, LocalDate.of(2026, 11, 1), "Pendiente", zonaId, zona, null);
    }

    private static CorteDeCaja armar(Integer zonaId, PedidoPorCobrar... pedidos) {
        return new ArmarCorteDeCajaService(() -> List.of(pedidos), RELOJ).armar(zonaId);
    }

    @Test
    @DisplayName("Luvianos: 3 pedidos sin pagar de $200, $350 y $100 suman $650 (cada uno con su número)")
    void sumaPorZona() {
        CorteDeCaja corte = armar(null,
                pedido(12, "200", "0", LUVIANOS, "Luvianos"),
                pedido(10, "350", "0", LUVIANOS, "Luvianos"),
                pedido(15, "100", "0", LUVIANOS, "Luvianos"));

        assertThat(corte.zonas()).hasSize(1);
        var luvianos = corte.zonas().get(0);
        assertThat(luvianos.zona()).isEqualTo("Luvianos");
        assertThat(luvianos.pedidos()).extracting(PedidoPorCobrar::pedidoId).containsExactly(10, 12, 15); // por número
        assertThat(luvianos.subtotal()).isEqualByComparingTo("650");
        assertThat(corte.total()).isEqualByComparingTo("650");
    }

    @Test
    @DisplayName("Cada pedido aporta lo que FALTA por cobrar: total menos lo abonado")
    void saldoEsTotalMenosAbonado() {
        CorteDeCaja corte = armar(null, pedido(1, "500", "120.50", LUVIANOS, "Luvianos"));
        assertThat(corte.zonas().get(0).pedidos().get(0).saldo()).isEqualByComparingTo("379.50");
        assertThat(corte.total()).isEqualByComparingTo("379.50");
    }

    @Test
    @DisplayName("Un pedido ya pagado completo no entra al corte")
    void pagadoNoEntra() {
        CorteDeCaja corte = armar(null,
                pedido(1, "500", "500", LUVIANOS, "Luvianos"),
                pedido(2, "300", "0", LUVIANOS, "Luvianos"));
        assertThat(corte.zonas().get(0).pedidos()).extracting(PedidoPorCobrar::pedidoId).containsExactly(2);
        assertThat(corte.total()).isEqualByComparingTo("300");
    }

    @Test
    @DisplayName("Un abono de más (pagado > total) tampoco entra y nunca resta")
    void abonoDeMasNoResta() {
        CorteDeCaja corte = armar(null, pedido(1, "100", "130", LUVIANOS, "Luvianos"), pedido(2, "50", "0", LUVIANOS, "Luvianos"));
        assertThat(corte.total()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("Varias zonas: por nombre, cada una con su subtotal, y «Sin zona» al final; el total suma todo")
    void variasZonasYSinZona() {
        CorteDeCaja corte = armar(null,
                pedido(1, "100", "0", null, null),
                pedido(2, "250", "0", TEJUPILCO, "Tejupilco"),
                pedido(3, "400", "0", LUVIANOS, "Luvianos"),
                pedido(4, "50", "0", TEJUPILCO, "Tejupilco"));

        assertThat(corte.zonas()).extracting(z -> z.zona()).containsExactly("Luvianos", "Tejupilco", null);
        assertThat(corte.zonas().get(0).subtotal()).isEqualByComparingTo("400");
        assertThat(corte.zonas().get(1).subtotal()).isEqualByComparingTo("300");
        assertThat(corte.zonas().get(2).subtotal()).isEqualByComparingTo("100");
        assertThat(corte.total()).isEqualByComparingTo("800");
        assertThat(corte.cantidadPedidos()).isEqualTo(4);
    }

    @Test
    @DisplayName("Con una zona elegida solo sale esa zona")
    void filtroPorZona() {
        CorteDeCaja corte = armar(TEJUPILCO,
                pedido(1, "100", "0", LUVIANOS, "Luvianos"),
                pedido(2, "250", "0", TEJUPILCO, "Tejupilco"));
        assertThat(corte.zonas()).hasSize(1);
        assertThat(corte.total()).isEqualByComparingTo("250");
    }

    @Test
    @DisplayName("Sin pedidos por cobrar: corte vacío con total 0")
    void vacio() {
        CorteDeCaja corte = armar(null);
        assertThat(corte.zonas()).isEmpty();
        assertThat(corte.total()).isEqualByComparingTo("0");
        assertThat(corte.generado()).isEqualTo(Instant.parse("2026-11-04T18:00:00Z"));
    }

    @Test
    @DisplayName("Los centavos suman exacto (sin errores de punto flotante)")
    void centavosExactos() {
        CorteDeCaja corte = armar(null,
                pedido(1, "0.10", "0", LUVIANOS, "Luvianos"),
                pedido(2, "0.20", "0", LUVIANOS, "Luvianos"));
        assertThat(corte.total()).isEqualByComparingTo("0.30");
    }
}
