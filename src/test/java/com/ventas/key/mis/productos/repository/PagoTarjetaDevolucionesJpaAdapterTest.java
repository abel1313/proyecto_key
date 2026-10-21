package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.Devolucion;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia.DevolucionesJpaAdapter;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia.IntentosDePagoJpaAdapter;
import com.ventas.key.mis.productos.entity.Pedido;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// Corre el adaptador de devoluciones contra H2 con la entidad real (mapeo y consultas derivadas).
@DataJpaTest
@ActiveProfiles("test")
@Import({DevolucionesJpaAdapter.class, IntentosDePagoJpaAdapter.class})
class PagoTarjetaDevolucionesJpaAdapterTest {

    @Autowired private DevolucionesJpaAdapter adaptador;
    @Autowired private IntentosDePagoJpaAdapter intentos;
    @Autowired private IPedidoRepository pedidos;
    @Autowired private EntityManager em;

    private Long intento() {
        Pedido p = new Pedido();
        p.setEstadoPedido("PENDIENTE");
        p.setTipoPedido("NORMAL");
        p.setTotalPedido(450.0);
        p.setTotalPagado(0.0);
        Integer pedidoId = pedidos.save(p).getId();
        return intentos.guardar(IntentoDePago.nuevo(pedidoId, 7, CanalDePago.TERMINAL,
                new BigDecimal("450.00"), 1, "admin", Instant.now())).id();
    }

    @Test
    void reservaQuedaPendienteYAlConfirmarDejaDeSerlo() {
        Long intentoId = intento();
        Devolucion reservada = adaptador.reservar(Devolucion.reservada(
                intentoId, new BigDecimal("100.00"), "admin", "dev-" + intentoId + "-0.00", Instant.now()));
        em.flush();
        em.clear();

        assertThat(reservada.id()).isNotNull();
        assertThat(reservada.confirmada()).isFalse();
        assertThat(adaptador.pendientesDelIntento(intentoId)).hasSize(1);

        Devolucion confirmada = adaptador.confirmar(reservada);
        em.flush();
        em.clear();
        assertThat(confirmada.confirmada()).isTrue();
        assertThat(adaptador.pendientesDelIntento(intentoId)).isEmpty();
        assertThat(adaptador.confirmar(reservada).confirmada()).isTrue();          // idempotente
    }
}
