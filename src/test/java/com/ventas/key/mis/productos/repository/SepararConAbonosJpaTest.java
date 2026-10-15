package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.grupopedido.infraestructura.salida.persistencia.AbonosDelGrupoJpaAdapter;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoRegistrado;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.Movimiento;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.ReacomodoDeAbonos;
import com.ventas.key.mis.productos.entity.AbonoPedido;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.service.AbonoServiceImpl;
import com.ventas.key.mis.productos.service.EmailService;
import com.ventas.key.mis.productos.service.RestockNotificacionService;
import com.ventas.key.mis.productos.service.WhatsappService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Separar un grupo a credito contra una base real (H2): los abonos se mueven de pedido y cada
 * pedido termina con su {@code total_pagado} recalculado. Las pruebas del dominio usan mocks y no
 * ven si Hibernate llega a escribir el movimiento antes de sumar.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({AbonosDelGrupoJpaAdapter.class, AbonoServiceImpl.class})
class SepararConAbonosJpaTest {

    @Autowired private IPedidoRepository pedidoRepository;
    @Autowired private IAbonoRepository abonoRepository;
    @Autowired private AbonosDelGrupoJpaAdapter adapter;
    @Autowired private AbonoServiceImpl abonoService;
    @Autowired private EntityManager em;

    @MockBean private EmailService emailService;
    @MockBean private WhatsappService whatsappService;
    @MockBean private RestockNotificacionService restockNotificacionService;

    private Pedido pedido(double total, double pagado, LocalDateTime registro) {
        Pedido p = new Pedido();
        p.setTipoPedido("APARTADO");
        p.setEstadoPedido("APARTADO");
        p.setTotalPedido(total);
        p.setTotalPagado(pagado);
        p.setFechaPedido(registro.toLocalDate());
        p.setFechaHoraRegistro(registro);
        return pedidoRepository.save(p);
    }

    private void abono(Pedido p, double monto) {
        AbonoPedido a = new AbonoPedido();
        a.setPedido(p);
        a.setMonto(monto);
        a.setFechaPago(LocalDate.now());
        a.setMetodoPago("EFECTIVO");
        a.setNota("Abono del grupo #1");
        abonoRepository.save(a);
    }

    @Test
    void separar_100_y_100_de_un_abono_de_200() {
        // El caso de QA: 200+80 y 198+200, abono de $200 al grupo (cae entero en el mas viejo).
        Pedido a = pedido(280.0, 200.0, LocalDateTime.now().minusHours(2));
        Pedido b = pedido(398.0, 0.0, LocalDateTime.now().minusHours(1));
        abono(a, 200.0);
        em.flush();

        List<AbonoRegistrado> abonos = adapter.abonosDe(List.of(a.getId(), b.getId()));
        Map<Integer, Long> objetivos = Map.of(a.getId(), 10_000L, b.getId(), 10_000L);
        for (Movimiento m : ReacomodoDeAbonos.planear(abonos, objetivos)) {
            adapter.mover(m, "Reparto al separar el grupo #1");
        }
        abonoService.ajustarEstadoALosAbonos(a.getId(), 1);
        abonoService.ajustarEstadoALosAbonos(b.getId(), 1);
        em.flush();
        em.clear();

        Pedido a2 = pedidoRepository.findById(a.getId()).orElseThrow();
        Pedido b2 = pedidoRepository.findById(b.getId()).orElseThrow();
        assertThat(a2.getTotalPagado()).isEqualTo(100.0);
        assertThat(b2.getTotalPagado()).isEqualTo(100.0);
        assertThat(a2.getTotalPedido() - a2.getTotalPagado()).isEqualTo(180.0);
        assertThat(b2.getTotalPedido() - b2.getTotalPagado()).isEqualTo(298.0);
        assertThat(abonoRepository.findByPedidoIdOrderByFechaPagoAsc(a.getId()))
                .extracting(AbonoPedido::getMonto).containsExactly(100.0);
        assertThat(abonoRepository.findByPedidoIdOrderByFechaPagoAsc(b.getId()))
                .extracting(AbonoPedido::getMonto).containsExactly(100.0);
    }
}
