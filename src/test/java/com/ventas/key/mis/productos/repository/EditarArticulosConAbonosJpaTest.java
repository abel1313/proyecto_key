package com.ventas.key.mis.productos.repository;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Despues de editar los articulos de un pedido a credito, su estado queda como dicen sus abonos
 * (R9 de pedidoarticulo), contra una base real (H2). La cuenta es solo de ese pedido.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(AbonoServiceImpl.class)
class EditarArticulosConAbonosJpaTest {

    @Autowired private IPedidoRepository pedidoRepository;
    @Autowired private IAbonoRepository abonoRepository;
    @Autowired private AbonoServiceImpl abonoService;
    @Autowired private EntityManager em;

    @MockBean private EmailService emailService;
    @MockBean private WhatsappService whatsappService;
    @MockBean private RestockNotificacionService restockNotificacionService;

    private Pedido pedido(String estado, double total, double abonado) {
        Pedido p = new Pedido();
        p.setTipoPedido("APARTADO");
        p.setEstadoPedido(estado);
        p.setTotalPedido(total);
        p.setTotalPagado(abonado);
        p.setFechaPedido(LocalDate.now());
        p.setFechaHoraRegistro(LocalDateTime.now());
        p = pedidoRepository.save(p);

        AbonoPedido a = new AbonoPedido();
        a.setPedido(p);
        a.setMonto(abonado);
        a.setFechaPago(LocalDate.now());
        a.setMetodoPago("EFECTIVO");
        abonoRepository.save(a);
        em.flush();
        return p;
    }

    private Pedido releer(Pedido p) {
        em.flush();
        em.clear();
        return pedidoRepository.findById(p.getId()).orElseThrow();
    }

    @Test
    void quitar_un_producto_que_aun_deja_deuda_solo_baja_lo_que_debe() {
        // Pedido C del ejemplo: 2 productos de $100 con $50 abonados; regresa uno -> total $100.
        Pedido c = pedido("APARTADO", 100.0, 50.0);

        abonoService.ajustarTrasEditarArticulos(c.getId(), 1);

        Pedido c2 = releer(c);
        assertThat(c2.getEstadoPedido()).isEqualTo("APARTADO");
        assertThat(c2.getTotalPagado()).isEqualTo(50.0);
        assertThat(c2.getTotalPedido() - c2.getTotalPagado()).isEqualTo(50.0);
    }

    @Test
    void un_pedido_pagado_que_cambia_a_algo_mas_caro_vuelve_a_deber() {
        // Estaba liquidado con $200 y le cambian un producto por uno $100 mas caro.
        Pedido p = pedido("PAGADO", 300.0, 200.0);

        abonoService.ajustarTrasEditarArticulos(p.getId(), 1);

        Pedido p2 = releer(p);
        assertThat(p2.getEstadoPedido()).isEqualTo("APARTADO");
        assertThat(p2.getTotalPedido() - p2.getTotalPagado()).isEqualTo(100.0);
    }

    @Test
    void un_pedido_de_contado_no_se_toca() {
        Pedido p = pedido("Pendiente", 300.0, 0.0);
        p.setTipoPedido("NORMAL");
        pedidoRepository.save(p);

        abonoService.ajustarTrasEditarArticulos(p.getId(), 1);

        assertThat(releer(p).getEstadoPedido()).isEqualTo("Pendiente");
    }
}
