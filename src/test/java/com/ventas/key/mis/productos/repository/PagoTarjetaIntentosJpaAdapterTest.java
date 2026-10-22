package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoIntento;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia.IntentosDePagoJpaAdapter;
import com.ventas.key.mis.productos.entity.Pedido;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Corre el adaptador contra H2 con la entidad real: si el mapeo de PagoTarjetaIntento o alguna
// consulta derivada estuviera mal, el back no arrancaria en el servidor.
@DataJpaTest
@ActiveProfiles("test")
@Import(IntentosDePagoJpaAdapter.class)
class PagoTarjetaIntentosJpaAdapterTest {

    @Autowired private IntentosDePagoJpaAdapter adaptador;
    @Autowired private IPedidoRepository pedidos;
    @Autowired private EntityManager em;
    @Autowired private IPagoTarjetaIntentoRepository repositorio;

    /** Fuerza ir a la base de verdad: sin esto, buscar() sale del cache de la sesion y no prueba nada. */
    private void vaciarCache() {
        em.flush();
        em.clear();
    }

    private Integer pedido() {
        Pedido p = new Pedido();
        p.setEstadoPedido("PENDIENTE");
        p.setTipoPedido("NORMAL");
        p.setTotalPedido(450.0);
        p.setTotalPagado(0.0);
        return pedidos.save(p).getId();
    }

    private IntentoDePago nuevo(Integer pedidoId, Instant ahora) {
        return IntentoDePago.nuevo(pedidoId, 77, CanalDePago.TERMINAL, new BigDecimal("450.00"), 3, "admin", ahora);
    }

    @Test
    void guardaYLeeUnIntentoConTodosSusCampos() {
        Integer pedidoId = pedido();
        IntentoDePago guardado = adaptador.guardar(nuevo(pedidoId, Instant.now()));
        vaciarCache();
        IntentoDePago leido = adaptador.buscar(guardado.id()).orElseThrow();
        assertThat(leido.clienteId()).isEqualTo(77);
        assertThat(leido.creadoEn()).isEqualTo(guardado.creadoEn());
        assertThat(leido.version()).isZero();
        assertThat(leido.canal()).isEqualTo(CanalDePago.TERMINAL);
        assertThat(leido.monto()).isEqualByComparingTo("450.00");
        assertThat(leido.cuotas()).isEqualTo(3);
        assertThat(leido.estado()).isEqualTo(EstadoIntento.CREADO);
    }

    @Test
    void actualizarSubeLaVersion_yUnaVersionVieja_esConflicto() {
        IntentoDePago guardado = adaptador.guardar(nuevo(pedido(), Instant.now()));
        vaciarCache();
        IntentoDePago enviado = adaptador.guardar(guardado.enviado("ext-1", Instant.now()));
        assertThat(enviado.version()).isEqualTo(1);
        vaciarCache();
        assertThatThrownBy(() -> adaptador.guardar(guardado.cancelado(Instant.now())))   // version 0 ya no es la actual
                .isInstanceOf(PagoConTarjetaException.ConflictoDeConcurrencia.class);
    }

    @Test
    void activoDelPedidoDevuelveSoloElQueSigueVivo() {
        Integer pedidoId = pedido();
        IntentoDePago creado = adaptador.guardar(nuevo(pedidoId, Instant.now()));
        assertThat(adaptador.activoDelPedido(pedidoId)).map(IntentoDePago::id).contains(creado.id());
        vaciarCache();
        adaptador.guardar(adaptador.buscar(creado.id()).orElseThrow().cancelado(Instant.now()));
        assertThat(adaptador.activoDelPedido(pedidoId)).isEmpty();
    }

    @Test
    void cuentaSoloRechazadosDentroDeLaVentana() {
        Integer pedidoId = pedido();
        IntentoDePago i = adaptador.guardar(nuevo(pedidoId, Instant.now()));
        vaciarCache();
        adaptador.guardar(adaptador.buscar(i.id()).orElseThrow().enviado("e", Instant.now()).rechazado(Instant.now()));
        assertThat(adaptador.rechazadosDesde(pedidoId, Instant.now().minus(Duration.ofHours(2)))).isEqualTo(1);
        assertThat(adaptador.rechazadosDesde(pedidoId, Instant.now().plus(Duration.ofHours(1)))).isZero();
    }

    @Test
    void activosCreadosAntesDeYPerdidosDesde() {
        Integer pedidoId = pedido();
        IntentoDePago viejo = adaptador.guardar(nuevo(pedidoId, Instant.now().minus(Duration.ofMinutes(20))));
        assertThat(adaptador.activosCreadosAntesDe(Instant.now().minus(Duration.ofMinutes(10))))
                .extracting(IntentoDePago::id).contains(viejo.id());
        assertThat(adaptador.activosCreadosAntesDe(Instant.now().minus(Duration.ofMinutes(30))))
                .extracting(IntentoDePago::id).doesNotContain(viejo.id());
        vaciarCache();
        IntentoDePago expirado = adaptador.guardar(adaptador.buscar(viejo.id()).orElseThrow().expirado(Instant.now()));
        assertThat(adaptador.perdidosDesde(Instant.now().minus(Duration.ofHours(1))))
                .extracting(IntentoDePago::id).contains(expirado.id());
    }

    @Test
    void existsPorPedidoYEstado_sirveParaBloquearCancelarOCambiarTipoDeUnPedidoConCobro() {
        Integer pedidoId = pedido();
        IntentoDePago i = adaptador.guardar(nuevo(pedidoId, Instant.now()));
        java.util.List<String> bloqueantes = java.util.List.of("CREADO", "ENVIADO", "APROBADO", "COBRADO_SIN_APLICAR", "DEVUELTO_PARCIAL");
        assertThat(repositorio.existsByPedidoIdAndEstadoIn(pedidoId, bloqueantes)).isTrue();          // activo
        vaciarCache();
        adaptador.guardar(adaptador.buscar(i.id()).orElseThrow().cancelado(Instant.now()));
        assertThat(repositorio.existsByPedidoIdAndEstadoIn(pedidoId, bloqueantes)).isFalse();         // cancelado: ya no bloquea
    }
}
