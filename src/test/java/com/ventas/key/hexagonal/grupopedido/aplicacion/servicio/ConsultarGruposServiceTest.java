package com.ventas.key.hexagonal.grupopedido.aplicacion.servicio;

import com.ventas.key.hexagonal.grupopedido.dominio.modelo.GrupoPedidos;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.PedidoDelGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.RegistroGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.GrupoPedidosPort;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida.PedidosDelGrupoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarGruposServiceTest {

    private final GrupoPedidosPort grupos = mock(GrupoPedidosPort.class);
    private final PedidosDelGrupoPort pedidos = mock(PedidosDelGrupoPort.class);
    private final ConsultarGruposService service = new ConsultarGruposService(grupos, pedidos);

    private static PedidoDelGrupo p(int id) {
        return new PedidoDelGrupo(id, "NORMAL", "Pendiente", 10_000, 0, LocalDateTime.of(2026, 9, id, 9, 0), "C" + id);
    }

    @Test
    @DisplayName("cada pedido de la pagina recibe su grupo, y cada grupo se lee una sola vez")
    void gruposActivosDe() {
        when(grupos.gruposActivosDe(List.of(1, 2, 9))).thenReturn(Map.of(1, 30, 2, 30));
        when(grupos.buscar(30)).thenReturn(Optional.of(
                new RegistroGrupo(30, 1, true, LocalDateTime.now(), null, List.of(1, 2))));
        when(pedidos.buscar(List.of(1, 2))).thenReturn(List.of(p(1), p(2)));

        Map<Integer, GrupoPedidos> r = service.gruposActivosDe(List.of(1, 2, 9));

        assertThat(r).containsOnlyKeys(1, 2);
        assertThat(r.get(1).grupoId()).isEqualTo(30);
        assertThat(r.get(2).totalCentavos()).isEqualTo(20_000);
        verify(grupos, times(1)).buscar(30);
    }
}
