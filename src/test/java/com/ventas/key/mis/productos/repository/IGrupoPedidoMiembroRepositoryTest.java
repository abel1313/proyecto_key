package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.GrupoPedido;
import com.ventas.key.mis.productos.entity.GrupoPedidoMiembro;
import com.ventas.key.mis.productos.entity.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Las @Query de grupos se validan al arrancar Spring: si alguna estuviera mal, el back no
// levantaria en el servidor. Esto las corre contra H2 antes de desplegar.
@DataJpaTest
@ActiveProfiles("test")
class IGrupoPedidoMiembroRepositoryTest {

    @Autowired private IPedidoRepository pedidoRepository;
    @Autowired private IGrupoPedidoRepository grupoRepository;
    @Autowired private IGrupoPedidoMiembroRepository miembroRepository;

    private Pedido pedido() {
        Pedido p = new Pedido();
        p.setEstadoPedido("FIADO");
        p.setTipoPedido("FIADO");
        p.setTotalPedido(100.0);
        p.setTotalPagado(0.0);
        return pedidoRepository.save(p);
    }

    private GrupoPedido grupo(boolean activo, Pedido... pedidos) {
        GrupoPedido g = new GrupoPedido();
        g.setPedidoTitular(pedidos[0]);
        g.setActivo(activo);
        g.setFechaCreacion(LocalDateTime.now());
        for (Pedido p : pedidos) {
            GrupoPedidoMiembro m = new GrupoPedidoMiembro();
            m.setGrupo(g);
            m.setPedido(p);
            g.getMiembros().add(m);
        }
        return grupoRepository.saveAndFlush(g);
    }

    @Test
    void solo_cuentan_los_grupos_activos() {
        Pedido a = pedido();
        Pedido b = pedido();
        Pedido c = pedido();
        Pedido suelto = pedido();
        grupo(false, a, c);
        GrupoPedido activo = grupo(true, a, b);

        List<Object[]> filas = miembroRepository.gruposActivosDe(List.of(a.getId(), b.getId(), c.getId(), suelto.getId()));

        assertThat(filas).extracting(f -> f[0]).containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(filas).extracting(f -> f[1]).containsOnly(activo.getId());
        assertThat(miembroRepository.estaEnGrupoActivo(a.getId())).isTrue();
        assertThat(miembroRepository.estaEnGrupoActivo(c.getId())).isFalse();
        assertThat(miembroRepository.estaEnGrupoActivo(suelto.getId())).isFalse();
    }

    @Test
    void los_miembros_se_conservan_al_deshacer() {
        Pedido a = pedido();
        Pedido b = pedido();
        GrupoPedido g = grupo(false, a, b);

        assertThat(miembroRepository.pedidosDelGrupo(g.getId())).containsExactly(a.getId(), b.getId());
    }
}
