package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.PedidoNoAgrupableException;
import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.PedidosDeDistintoTipoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnionDePedidosTest {

    static PedidoDelGrupo pedido(int id, String tipo, String estado) {
        return new PedidoDelGrupo(id, tipo, estado, 10_000, 0, LocalDateTime.of(2026, 9, 1, 10, 0).plusDays(id), "Cliente " + id);
    }

    @Test
    @DisplayName("tres pedidos abiertos del mismo tipo se pueden unir")
    void seUnen() {
        List<PedidoDelGrupo> p = List.of(pedido(1, "FIADO", "FIADO"), pedido(2, "FIADO", "FIADO"), pedido(3, "FIADO", "FIADO"));
        assertThatCode(() -> UnionDePedidos.validar(List.of(1, 2, 3), p, 2, Map.of())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("se necesitan al menos 2 pedidos distintos; repetir el mismo no cuenta")
    void minimoDos() {
        assertThatThrownBy(() -> UnionDePedidos.validar(List.of(1, 1), List.of(pedido(1, "FIADO", "FIADO")), 1, Map.of()))
                .isInstanceOf(PedidoNoAgrupableException.class).hasMessageContaining("al menos 2");
    }

    @Test
    @DisplayName("un pedido que no existe se nombra en el error")
    void noExiste() {
        assertThatThrownBy(() -> UnionDePedidos.validar(List.of(1, 9), List.of(pedido(1, "FIADO", "FIADO")), 1, Map.of()))
                .hasMessageContaining("[9]");
    }

    @Test
    @DisplayName("el titular tiene que ser uno de los pedidos")
    void titularDeFuera() {
        List<PedidoDelGrupo> p = List.of(pedido(1, "FIADO", "FIADO"), pedido(2, "FIADO", "FIADO"));
        assertThatThrownBy(() -> UnionDePedidos.validar(List.of(1, 2), p, 7, Map.of()))
                .hasMessageContaining("titular");
    }

    @Test
    @DisplayName("un pedido cobrado de contado o cancelado no se une")
    void cerrados() {
        for (String estado : List.of("Entregado", "cancelado")) {
            List<PedidoDelGrupo> p = List.of(pedido(1, "FIADO", "FIADO"), pedido(2, "FIADO", estado));
            assertThatThrownBy(() -> UnionDePedidos.validar(List.of(1, 2), p, 1, Map.of()))
                    .isInstanceOf(PedidoNoAgrupableException.class).hasMessageContaining("#2");
        }
    }

    @Test
    @DisplayName("un pedido a credito ya pagado si se une: su dinero pasa a ser del grupo")
    void creditoPagadoSeUne() {
        List<PedidoDelGrupo> p = List.of(pedido(1, "FIADO", "FIADO"), pedido(2, "FIADO", "PAGADO"));
        assertThatCode(() -> UnionDePedidos.validar(List.of(1, 2), p, 1, Map.of())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("al contado ya cobrado se le dice que primero lo pase a credito")
    void contadoCobradoSeExplica() {
        List<PedidoDelGrupo> p = List.of(pedido(1, "NORMAL", "Pendiente"), pedido(2, "NORMAL", "Entregado"));
        assertThatThrownBy(() -> UnionDePedidos.validar(List.of(1, 2), p, 1, Map.of()))
                .hasMessageContaining("Cambiar forma de cobro");
    }

    @Test
    @DisplayName("un pedido que ya esta en otro grupo activo no se une")
    void yaEnGrupo() {
        List<PedidoDelGrupo> p = List.of(pedido(1, "FIADO", "FIADO"), pedido(2, "FIADO", "FIADO"));
        assertThatThrownBy(() -> UnionDePedidos.validar(List.of(1, 2), p, 1, Map.of(2, 40)))
                .hasMessageContaining("grupo #40");
    }

    @Test
    @DisplayName("distinta forma de cobro: error con el tipo de cada pedido para poder corregirlo")
    void distintoTipo() {
        List<PedidoDelGrupo> p = List.of(pedido(1, "FIADO", "FIADO"), pedido(2, "APARTADO", "APARTADO"));
        assertThatThrownBy(() -> UnionDePedidos.validar(List.of(1, 2), p, 1, Map.of()))
                .isInstanceOfSatisfying(PedidosDeDistintoTipoException.class, e -> {
                    assertThat(e.tipoPorPedido()).containsEntry(1, "FIADO").containsEntry(2, "APARTADO");
                    assertThat(e.getMessage()).contains("Ir pagando").contains("Apartado").contains("Cambia la forma de cobro");
                });
    }

    // ── Agregar a un grupo que ya existe (R18) ─────────────────────────────────────────

    static GrupoPedidos grupo(boolean activo, PedidoDelGrupo... ps) {
        return new GrupoPedidos(30, ps[0].pedidoId(), activo, LocalDateTime.now(), null, List.of(ps));
    }

    @Test
    @DisplayName("agregar un tercero con la misma forma de cobro se puede")
    void agregaTercero() {
        GrupoPedidos g = grupo(true, pedido(1, "FIADO", "FIADO"), pedido(2, "FIADO", "FIADO"));
        assertThatCode(() -> UnionDePedidos.validarAgregado(g, List.of(3), List.of(pedido(3, "FIADO", "FIADO")), Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("los que ya estan pueden estar pagados o cancelados: solo se validan los que entran")
    void soloSeValidanLosQueEntran() {
        GrupoPedidos g = grupo(true, pedido(1, "FIADO", "PAGADO"), pedido(2, "FIADO", "cancelado"), pedido(4, "FIADO", "FIADO"));
        assertThatCode(() -> UnionDePedidos.validarAgregado(g, List.of(3), List.of(pedido(3, "FIADO", "FIADO")), Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("agregar uno de otra forma de cobro dice el tipo del grupo y el del pedido")
    void agregaOtroTipo() {
        GrupoPedidos g = grupo(true, pedido(1, "FIADO", "FIADO"), pedido(2, "FIADO", "FIADO"));
        assertThatThrownBy(() -> UnionDePedidos.validarAgregado(g, List.of(3), List.of(pedido(3, "APARTADO", "APARTADO")), Map.of()))
                .isInstanceOf(PedidosDeDistintoTipoException.class)
                .satisfies(e -> assertThat(((PedidosDeDistintoTipoException) e).tipoPorPedido())
                        .containsEntry(1, "FIADO").containsEntry(3, "APARTADO"));
    }

    @Test
    @DisplayName("no se agrega a un grupo separado, ni uno que ya esta, ni uno de otro grupo, ni uno cerrado")
    void agregaNo() {
        GrupoPedidos separado = grupo(false, pedido(1, "FIADO", "FIADO"), pedido(2, "FIADO", "FIADO"));
        assertThatThrownBy(() -> UnionDePedidos.validarAgregado(separado, List.of(3), List.of(pedido(3, "FIADO", "FIADO")), Map.of()))
                .hasMessageContaining("ya se separo");

        GrupoPedidos g = grupo(true, pedido(1, "FIADO", "FIADO"), pedido(2, "FIADO", "FIADO"));
        assertThatThrownBy(() -> UnionDePedidos.validarAgregado(g, List.of(2), List.of(pedido(2, "FIADO", "FIADO")), Map.of()))
                .hasMessageContaining("Ya estan en este grupo");
        assertThatThrownBy(() -> UnionDePedidos.validarAgregado(g, List.of(3), List.of(pedido(3, "FIADO", "FIADO")), Map.of(3, 40)))
                .hasMessageContaining("grupo #40");
        assertThatThrownBy(() -> UnionDePedidos.validarAgregado(g, List.of(3), List.of(pedido(3, "FIADO", "cancelado")), Map.of()))
                .hasMessageContaining("#3");
        assertThatThrownBy(() -> UnionDePedidos.validarAgregado(g, List.of(9), List.of(), Map.of()))
                .hasMessageContaining("[9]");
        assertThatThrownBy(() -> UnionDePedidos.validarAgregado(g, List.of(), List.of(), Map.of()))
                .hasMessageContaining("al menos un pedido");
    }
}
