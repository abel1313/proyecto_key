package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeparacionTest {

    private static PedidoDelGrupo p(int id, String tipo, String estado, long total, long pagado) {
        return new PedidoDelGrupo(id, tipo, estado, total, pagado, LocalDateTime.of(2026, 9, id, 9, 0), "C" + id);
    }

    private static GrupoPedidos grupo(int titular, PedidoDelGrupo... ps) {
        return new GrupoPedidos(30, titular, true, LocalDateTime.now(), null, List.of(ps));
    }

    private static final List<AbonoRegistrado> CIEN = List.of(new AbonoRegistrado(1, 1, 10_000, LocalDate.of(2026, 9, 5)));

    @Test
    @DisplayName("separar todos: cada uno con lo que se escribio, y tiene que sumar lo abonado")
    void todos() {
        GrupoPedidos g = grupo(1, p(1, "FIADO", "PAGADO", 10_000, 10_000), p(2, "FIADO", "FIADO", 10_000, 0));
        PlanDeSeparacion plan = Separacion.planear(g, CIEN, List.of(1, 2), Map.of(1, 1_000L, 2, 9_000L), null);
        assertThat(plan.terminaElGrupo()).isTrue();
        assertThat(plan.objetivos()).containsEntry(1, 1_000L).containsEntry(2, 9_000L);
    }

    @Test
    @DisplayName("si al separar uno queda uno solo, el grupo se termina y el que queda se lleva el resto")
    void quedaUnoSolo() {
        GrupoPedidos g = grupo(1, p(1, "FIADO", "FIADO", 10_000, 10_000), p(2, "FIADO", "FIADO", 10_000, 0));
        PlanDeSeparacion plan = Separacion.planear(g, CIEN, List.of(2), Map.of(2, 2_500L), null);
        assertThat(plan.terminaElGrupo()).isTrue();
        assertThat(plan.salen()).containsExactlyInAnyOrder(1, 2);
        assertThat(plan.objetivos()).containsEntry(2, 2_500L).containsEntry(1, 7_500L);
    }

    @Test
    @DisplayName("a un pedido no se le deja mas de lo que cuesta")
    void noMasQueSuTotal() {
        GrupoPedidos g = grupo(1, p(1, "FIADO", "FIADO", 5_000, 5_000), p(2, "FIADO", "FIADO", 20_000, 5_000));
        List<AbonoRegistrado> abonos = List.of(new AbonoRegistrado(1, 1, 5_000, LocalDate.now()),
                new AbonoRegistrado(2, 2, 5_000, LocalDate.now()));
        assertThatThrownBy(() -> Separacion.planear(g, abonos, List.of(1, 2), Map.of(1, 10_000L, 2, 0L), null))
                .hasMessageContaining("cuesta $50.00");
    }

    @Test
    @DisplayName("si se separa el que recoge y siguen unidos otros, hay que elegir quien recoge")
    void titularQueSale() {
        GrupoPedidos g = grupo(1, p(1, "FIADO", "FIADO", 10_000, 10_000), p(2, "FIADO", "FIADO", 10_000, 0),
                p(3, "FIADO", "FIADO", 10_000, 0));
        assertThatThrownBy(() -> Separacion.planear(g, CIEN, List.of(1), Map.of(1, 10_000L), null))
                .hasMessageContaining("elige quien recoge");
        PlanDeSeparacion plan = Separacion.planear(g, CIEN, List.of(1), Map.of(1, 10_000L), 3);
        assertThat(plan.titular()).isEqualTo(3);
        assertThat(plan.quedan()).containsExactly(2, 3);
    }

    @Test
    @DisplayName("un grupo de contado se separa sin repartir dinero")
    void contado() {
        GrupoPedidos g = grupo(1, p(1, "NORMAL", "Pendiente", 10_000, 0), p(2, "NORMAL", "Pendiente", 10_000, 0));
        PlanDeSeparacion plan = Separacion.planear(g, List.of(), List.of(1, 2), Map.of(), null);
        assertThat(plan.objetivos()).isEmpty();
        assertThatThrownBy(() -> Separacion.planear(g, List.of(), List.of(1, 2), Map.of(1, 500L), null))
                .hasMessageContaining("contado");
    }
}
