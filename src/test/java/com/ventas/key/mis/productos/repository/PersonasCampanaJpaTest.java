package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.campana.dominio.modelo.PersonaCampana;
import com.ventas.key.hexagonal.campana.infraestructura.salida.persistencia.PersonasCampanaJpaAdapter;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import com.ventas.key.mis.productos.entity.Pedido;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** El adaptador de personas de campaña contra una base real (H2): guardar, buscar por código y contacto, acumular y borrar. */
@DataJpaTest
@ActiveProfiles("test")
@Import({PersonasCampanaJpaAdapter.class, TablasDisponibles.class})
class PersonasCampanaJpaTest {

    @Autowired private PersonasCampanaJpaAdapter adaptador;
    @Autowired private EntityManager em;
    @Autowired private IPedidoRepository pedidoRepository;

    /** Un pedido de verdad: solo suman hacia la meta los que existen y no estan Pendientes ni cancelados. */
    private int pedido(String estado) {
        Pedido p = new Pedido();
        p.setTipoPedido("NORMAL");
        p.setEstadoPedido(estado);
        p.setTotalPedido(0.0);
        p.setTotalPagado(0.0);
        p.setFechaPedido(java.time.LocalDate.now());
        p.setFechaHoraRegistro(java.time.LocalDateTime.now());
        p.setDetalles(new java.util.ArrayList<>());
        return pedidoRepository.save(p).getId();
    }

    @Test
    void guardaBuscaAcumulaYBorra() {
        PersonaCampana ana = adaptador.guardar(new PersonaCampana(null, "ABC-123", "Ana", "Ana@Mail.com", "(722) 123-4567", true));
        em.flush();
        em.clear();
        assertThat(ana.id()).isNotNull();

        assertThat(adaptador.porUuid("abc-123")).isPresent();
        assertThat(adaptador.porUuid("otro")).isEmpty();
        assertThat(adaptador.porContacto("ana@mail.com", null)).isPresent();
        assertThat(adaptador.porContacto(null, "7221234567")).as("el teléfono se compara por los últimos 10 dígitos").isPresent();
        assertThat(adaptador.porContacto("nadie@mail.com", "5550000000")).isEmpty();

        adaptador.ligarPedido(1, ana.id(), pedido("Entregado"), new BigDecimal("500"));
        adaptador.ligarPedido(1, ana.id(), pedido("PAGADO"), new BigDecimal("250.50"));
        adaptador.ligarPedido(2, ana.id(), pedido("Entregado"), new BigDecimal("999"));
        adaptador.ligarPedido(1, ana.id(), pedido("Pendiente"), new BigDecimal("4000"));   // tarjeta sin aprobar: no cuenta
        adaptador.ligarPedido(1, ana.id(), pedido("cancelado"), new BigDecimal("3000"));   // cancelado: no cuenta
        em.flush();
        assertThat(adaptador.acumulado(1, ana.id())).isEqualByComparingTo("750.50");
        assertThat(adaptador.acumulado(1, 999)).isEqualByComparingTo("0");

        adaptador.eliminar(ana.id());
        em.flush();
        em.clear();
        assertThat(adaptador.porUuid("abc-123")).isEmpty();
        assertThat(adaptador.acumulado(1, ana.id())).isEqualByComparingTo("0");
    }

    @Test
    void reporteYPedidoDeLaPersona() {
        PersonaCampana ana = adaptador.guardar(new PersonaCampana(null, "u-ana", "Ana", "ana@m.com", null, true));
        PersonaCampana beto = adaptador.guardar(new PersonaCampana(null, "u-beto", "Beto", null, "5551112222", true));
        int p101 = pedido("Entregado");
        adaptador.ligarPedido(1, ana.id(), pedido("Entregado"), new BigDecimal("700"));
        adaptador.ligarPedido(1, ana.id(), p101, new BigDecimal("900"));
        adaptador.ligarPedido(1, ana.id(), pedido("cancelado"), new BigDecimal("800"));   // cancelado: no cuenta
        adaptador.ligarPedido(1, beto.id(), pedido("PAGADO"), new BigDecimal("300"));
        adaptador.ligarPedido(2, beto.id(), pedido("Entregado"), new BigDecimal("5000"));   // otra campana: no cuenta aqui
        em.flush();
        em.clear();

        var resumen = adaptador.resumen(1);
        assertThat(resumen).hasSize(2);
        assertThat(resumen.get(0).persona().nombre()).as("la que mas lleva, primero").isEqualTo("Ana");
        assertThat(resumen.get(0).acumulado()).isEqualByComparingTo("1600");
        assertThat(resumen.get(0).pedidos()).isEqualTo(2);
        assertThat(resumen.get(1).acumulado()).isEqualByComparingTo("300");

        assertThat(adaptador.dePedido(p101)).get().extracting(PersonaCampana::nombre).isEqualTo("Ana");
        assertThat(adaptador.dePedido(999)).isEmpty();
    }
}
