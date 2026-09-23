package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReacomodoDeAbonosTest {

    private static AbonoRegistrado a(int id, int pedido, long centavos, int dia) {
        return new AbonoRegistrado(id, pedido, centavos, LocalDate.of(2026, 9, dia));
    }

    @Test
    @DisplayName("si ya esta como se pidio no se mueve nada")
    void nadaQueMover() {
        assertThat(ReacomodoDeAbonos.planear(List.of(a(1, 10, 5_000, 1)), Map.of(10, 5_000L, 11, 0L))).isEmpty();
    }

    @Test
    @DisplayName("un abono de $100 se parte 40 / 30 / 30 entre tres pedidos")
    void parteUnAbono() {
        List<Movimiento> m = ReacomodoDeAbonos.planear(List.of(a(1, 10, 10_000, 1)),
                Map.of(10, 4_000L, 11, 3_000L, 12, 3_000L));
        assertThat(m).containsExactly(new Movimiento(1, 10, 11, 3_000), new Movimiento(1, 10, 12, 3_000));
    }

    @Test
    @DisplayName("del pedido que sobra se mueven primero los abonos mas nuevos")
    void primeroLosNuevos() {
        List<Movimiento> m = ReacomodoDeAbonos.planear(List.of(a(1, 10, 5_000, 1), a(2, 10, 5_000, 9)),
                Map.of(10, 5_000L, 11, 5_000L));
        assertThat(m).containsExactly(new Movimiento(2, 10, 11, 5_000));
    }

    @Test
    @DisplayName("lo repartido tiene que sumar exacto lo abonado")
    void exacto() {
        assertThatThrownBy(() -> ReacomodoDeAbonos.planear(List.of(a(1, 10, 10_000, 1)), Map.of(10, 5_000L, 11, 4_000L)))
                .hasMessageContaining("igual a lo abonado");
    }
}
