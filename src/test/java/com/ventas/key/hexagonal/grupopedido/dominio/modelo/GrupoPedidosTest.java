package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.AbonoAlGrupoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrupoPedidosTest {

    private static PedidoDelGrupo p(int id, String estado, long total, long pagado, int dia) {
        return new PedidoDelGrupo(id, "FIADO", estado, total, pagado, LocalDateTime.of(2026, 9, dia, 12, 0), "C" + id);
    }

    private static GrupoPedidos grupo(PedidoDelGrupo... pedidos) {
        return new GrupoPedidos(1, pedidos[0].pedidoId(), true, LocalDateTime.now(), null, List.of(pedidos));
    }

    @Test
    @DisplayName("el saldo del grupo es la suma de lo que falta en cada pedido; los cancelados no cuentan")
    void saldo() {
        GrupoPedidos g = grupo(p(1, "FIADO", 50_000, 10_000, 1), p(2, "FIADO", 30_000, 0, 2), p(3, "cancelado", 99_900, 0, 3));
        assertThat(g.totalCentavos()).isEqualTo(80_000);
        assertThat(g.pagadoCentavos()).isEqualTo(10_000);
        assertThat(g.saldoCentavos()).isEqualTo(70_000);
    }

    @Test
    @DisplayName("el abono llena primero el pedido mas viejo, aunque se haya pasado en otro orden")
    void repartePorAntiguedad() {
        GrupoPedidos g = grupo(p(2, "FIADO", 30_000, 0, 5), p(1, "FIADO", 50_000, 10_000, 1));
        List<Reparto> r = g.repartir(50_000);
        assertThat(r).containsExactly(new Reparto(1, 40_000, true), new Reparto(2, 10_000, false));
    }

    @Test
    @DisplayName("un abono chico solo toca al primero")
    void abonoChico() {
        GrupoPedidos g = grupo(p(1, "FIADO", 50_000, 0, 1), p(2, "FIADO", 30_000, 0, 2));
        assertThat(g.repartir(10_000)).containsExactly(new Reparto(1, 10_000, false));
    }

    @Test
    @DisplayName("se salta los pedidos ya pagados y los cancelados")
    void saltaCerrados() {
        GrupoPedidos g = grupo(p(1, "PAGADO", 50_000, 50_000, 1), p(2, "cancelado", 10_000, 0, 2), p(3, "FIADO", 30_000, 0, 3));
        assertThat(g.repartir(30_000)).containsExactly(new Reparto(3, 30_000, true));
    }

    @Test
    @DisplayName("no se abona mas que el saldo del grupo, ni cero")
    void limites() {
        GrupoPedidos g = grupo(p(1, "FIADO", 50_000, 0, 1), p(2, "FIADO", 30_000, 0, 2));
        assertThatThrownBy(() -> g.repartir(80_001)).isInstanceOf(AbonoAlGrupoInvalidoException.class).hasMessageContaining("excede");
        assertThatThrownBy(() -> g.repartir(0)).hasMessageContaining("mayor a cero");
    }

    @Test
    @DisplayName("un grupo de contado no recibe abonos")
    void contado() {
        GrupoPedidos g = new GrupoPedidos(1, 1, true, LocalDateTime.now(), null, List.of(
                new PedidoDelGrupo(1, "NORMAL", "Pendiente", 10_000, 0, LocalDateTime.now(), "A"),
                new PedidoDelGrupo(2, "NORMAL", "Pendiente", 10_000, 0, LocalDateTime.now(), "B")));
        assertThatThrownBy(() -> g.repartir(5_000)).hasMessageContaining("contado");
    }

    @Test
    @DisplayName("un grupo deshecho no recibe abonos")
    void deshecho() {
        GrupoPedidos g = new GrupoPedidos(1, 1, false, LocalDateTime.now(), null, List.of(p(1, "FIADO", 10_000, 0, 1)));
        assertThatThrownBy(() -> g.repartir(5_000)).hasMessageContaining("ya se deshizo");
    }

    @Test
    @DisplayName("abono al grupo: tarjeta no, y el efectivo entregado cubre el abono completo")
    void abonoAlGrupo() {
        assertThatThrownBy(() -> new AbonoAlGrupo(1_000, "TARJETA", null, null)).hasMessageContaining("EFECTIVO o TRANSFERENCIA");
        assertThat(new AbonoAlGrupo(35_000, null, 50_000L, null).cambioCentavos()).isEqualTo(15_000);
        assertThat(new AbonoAlGrupo(35_000, "TRANSFERENCIA", 50_000L, null).cambioCentavos()).isZero();
        assertThatThrownBy(() -> new AbonoAlGrupo(35_000, "EFECTIVO", 20_000L, null).cambioCentavos()).hasMessageContaining("menor");
    }
}
