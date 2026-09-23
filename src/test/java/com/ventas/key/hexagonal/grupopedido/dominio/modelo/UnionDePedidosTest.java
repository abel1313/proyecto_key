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
}
