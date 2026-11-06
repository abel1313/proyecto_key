package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.cortecaja.dominio.modelo.FiltroDeCorte;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.PedidoPorCobrar;
import com.ventas.key.hexagonal.cortecaja.infraestructura.salida.persistencia.PedidosAbiertosJpaAdapter;
import com.ventas.key.mis.productos.entity.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regla del dueño (2026-11-05): las fechas del Corte de caja son la FECHA DE ENTREGA que se programa en Entregas por zona
 * ({@code fecha_recogida}); si el pedido no tiene fecha de entrega (apartado, ir pagando, ya lo recogieron sin fecha), cuenta la fecha en que se vendió.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(PedidosAbiertosJpaAdapter.class)
class PedidosAbiertosPorFechaDeEntregaJpaTest {

    private static final LocalDate DOMINGO = LocalDate.of(2026, 11, 8);

    @Autowired private IPedidoRepository pedidoRepository;
    @Autowired private PedidosAbiertosJpaAdapter adapter;

    private int pedido(LocalDate hecho, LocalDate entrega) {
        Pedido p = new Pedido();
        p.setTipoPedido("NORMAL");
        p.setEstadoPedido("Pendiente");
        p.setTotalPedido(100.0);
        p.setTotalPagado(0.0);
        p.setFechaPedido(hecho);
        p.setFechaRecogida(entrega);
        return pedidoRepository.save(p).getId();
    }

    private List<Integer> ids(LocalDate desde, LocalDate hasta) {
        var filtro = new FiltroDeCorte(Set.of(), desde, hasta, FiltroDeCorte.Pago.TODOS, FiltroDeCorte.Entrega.TODOS);
        return adapter.buscar(filtro).stream().map(PedidoPorCobrar::pedidoId).toList();
    }

    @Test
    void conFechas_salenLosQueSeEntreganEsosDias_aunqueSeHayanPedidoAntes() {
        int pedidoHaceDosSemanasParaElDomingo = pedido(LocalDate.of(2026, 10, 25), DOMINGO);
        int pedidoDeHoyParaOtroDia = pedido(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 15));
        int sinFechaVendidoOtroDia = pedido(LocalDate.of(2026, 11, 5), null);

        assertThat(ids(DOMINGO, DOMINGO)).containsExactly(pedidoHaceDosSemanasParaElDomingo);
        assertThat(ids(DOMINGO, DOMINGO)).doesNotContain(pedidoDeHoyParaOtroDia, sinFechaVendidoOtroDia);
    }

    @Test
    void sinFechaDeEntrega_cuentaLaFechaEnQueSeVendio() {
        int vendidoEnLaSemanaSinFecha = pedido(LocalDate.of(2026, 11, 4), null);
        int vendidoAntesSinFecha = pedido(LocalDate.of(2026, 10, 20), null);
        LocalDate lunes = LocalDate.of(2026, 11, 2);

        assertThat(ids(lunes, DOMINGO)).contains(vendidoEnLaSemanaSinFecha).doesNotContain(vendidoAntesSinFecha);
    }

    @Test
    void conFechaDeEntrega_manda_aunqueSeHayaVendidoEnOtraSemana() {
        int vendidoLaSemanaPasadaEntregadoEstaSemana = pedido(LocalDate.of(2026, 10, 28), LocalDate.of(2026, 11, 4));
        int vendidoEstaSemanaEntregaLaSiguiente = pedido(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 12));
        LocalDate lunes = LocalDate.of(2026, 11, 2);

        assertThat(ids(lunes, DOMINGO)).contains(vendidoLaSemanaPasadaEntregadoEstaSemana).doesNotContain(vendidoEstaSemanaEntregaLaSiguiente);
    }

    @Test
    void sinFechas_salenTodos_tambienLosQueNoTienenFechaDeEntrega() {
        int a = pedido(LocalDate.of(2026, 11, 1), DOMINGO);
        int b = pedido(LocalDate.of(2026, 11, 1), null);
        assertThat(ids(null, null)).contains(a, b);
    }

    @Test
    void elRango_incluyeSusDosExtremos() {
        int lunes = pedido(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));
        int domingo = pedido(LocalDate.of(2026, 11, 1), DOMINGO);
        int fuera = pedido(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 9));
        assertThat(ids(LocalDate.of(2026, 11, 2), DOMINGO)).contains(lunes, domingo).doesNotContain(fuera);
    }
}
