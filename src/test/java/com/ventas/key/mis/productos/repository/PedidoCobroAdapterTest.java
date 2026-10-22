package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PedidoACobrar;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ResultadoAplicacion;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.TipoDeCobro;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia.IntentosDePagoJpaAdapter;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia.PedidoCobroAdapter;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.MesesIntereses;
import com.ventas.key.mis.productos.entity.PagosYMeses;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.TipoPago;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El adaptador del pedido contra H2 con las entidades reales: lo que lee del pedido y, sobre todo, lo
 * que escribe al marcarlo pagado (pedido, venta, entrega segun el canal) y que repetirlo no duplica nada.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({PedidoCobroAdapter.class, IntentosDePagoJpaAdapter.class})
class PedidoCobroAdapterTest {

    @Autowired private PedidoCobroAdapter adaptador;
    @Autowired private IntentosDePagoJpaAdapter intentos;
    @Autowired private IPedidoRepository pedidos;
    @Autowired private IDetallePedidoRepository detalles;
    @Autowired private IProductosRepository productos;
    @Autowired private IVarianteRepository variantes;
    @Autowired private ICodigoBarrasRepository codigos;
    @Autowired private IUsuarioRepository usuarios;
    @Autowired private IVentaRepository ventas;
    @Autowired private EntityManager em;

    @BeforeEach
    void preparar() {
        // Las entidades todavia no mapean acepta_msi; la tabla real si la tiene (migration_pago_tarjeta.sql).
        em.createNativeQuery("ALTER TABLE producto ADD COLUMN IF NOT EXISTS acepta_msi TINYINT DEFAULT 0 NOT NULL").executeUpdate();
        em.createNativeQuery("ALTER TABLE variantes ADD COLUMN IF NOT EXISTS acepta_msi TINYINT NULL").executeUpdate();

        TipoPago tipo = new TipoPago();
        tipo.setFormaPago("Tarjeta Debito");
        em.persist(tipo);
        TipoPago credito = new TipoPago();
        credito.setFormaPago("Tarjeta credito");
        em.persist(credito);
        MesesIntereses tresMeses = new MesesIntereses();
        tresMeses.setMeses("3");
        tresMeses.setDescripcion("Pago tarjeta de crédito a 3 meses sin intereses");
        em.persist(tresMeses);
        PagosYMeses formaTres = new PagosYMeses();
        formaTres.setTipoPago(credito);
        formaTres.setMesesIntereses(tresMeses);
        em.persist(formaTres);
        MesesIntereses contado = new MesesIntereses();
        contado.setMeses("1");
        contado.setDescripcion("Contado");
        em.persist(contado);
        PagosYMeses forma = new PagosYMeses();
        forma.setTipoPago(tipo);
        forma.setMesesIntereses(contado);
        em.persist(forma);
        em.flush();
        em.createNativeQuery("UPDATE pagos_y_meses SET id = 2 WHERE id = " + forma.getId()).executeUpdate();

        Usuario u = new Usuario();
        u.setUsername("admin");
        u.setPassword("x");
        usuarios.save(u);
        em.flush();
        em.clear();
    }

    private Pedido pedido(String tipo, String estado, Variantes v) {
        Pedido p = new Pedido();
        p.setTipoPedido(tipo);
        p.setEstadoPedido(estado);
        p.setTotalPedido(450.0);
        p.setTotalPagado(0.0);
        p.setFechaPedido(LocalDate.now());
        p.setFechaHoraRegistro(LocalDateTime.now());
        p = pedidos.save(p);
        DetallePedido d = new DetallePedido();
        d.setPedido(p);
        d.setProducto(v.getProducto());
        d.setVariante(v);
        d.setCantidad(1);
        d.setPrecioUnitario(450.0);
        d.setSubTotal(450.0);
        detalles.save(d);
        em.flush();
        em.clear();
        return pedidos.findById(p.getId()).orElseThrow();
    }

    private Variantes articulo(boolean productoAceptaMsi) {
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras("750" + System.nanoTime());
        cb = codigos.save(cb);
        Producto pr = new Producto();
        pr.setNombre("Bolsa");
        pr.setStock(10);
        pr.setHabilitado('1');
        pr.setPrecioCosto(100.0);
        pr.setPrecioVenta(450.0);
        pr.setPrecioRebaja(0.0);
        pr.setPiezas(0.0);
        pr.setCodigoBarras(cb);
        pr.setEsCatalogoInterno(false);
        pr = productos.save(pr);
        Variantes v = new Variantes();
        v.setProducto(pr);
        v.setTalla("U");
        v.setStock(10);
        v = variantes.save(v);
        em.flush();
        if (productoAceptaMsi) {
            em.createNativeQuery("UPDATE producto SET acepta_msi = 1 WHERE id = " + pr.getId()).executeUpdate();
        }
        em.clear();
        return v;
    }

    private IntentoDePago intento(Integer pedidoId, CanalDePago canal, int cuotas) {
        return intentos.guardar(IntentoDePago.nuevo(pedidoId, null, canal, new BigDecimal("450.00"), cuotas, "admin", Instant.now()));
    }

    @Test
    void leeElPedidoConSuTipoTotalYLaCasillaDeMsiHeredadaDelProducto() {
        Pedido p = pedido("NORMAL", "PENDIENTE", articulo(true));
        PedidoACobrar r = adaptador.buscar(p.getId()).orElseThrow();
        assertThat(r.tipo()).isEqualTo(TipoDeCobro.CONTADO);
        assertThat(r.total()).isEqualByComparingTo("450.00");
        assertThat(r.pagado()).isFalse();
        assertThat(r.cancelado()).isFalse();
        assertThat(r.articulos()).hasSize(1);
        assertThat(r.articulos().get(0).aceptaMsi()).isTrue();

        Pedido sinMsi = pedido("NORMAL", "PENDIENTE", articulo(false));
        assertThat(adaptador.buscar(sinMsi.getId()).orElseThrow().articulos().get(0).aceptaMsi()).isFalse();
    }

    @Test
    void laDecisionPropiaDelArticuloGanaSobreLaDelProducto() {
        Variantes v = articulo(true);
        em.createNativeQuery("UPDATE variantes SET acepta_msi = 0 WHERE id = " + v.getId()).executeUpdate();
        em.clear();
        Pedido p = pedido("NORMAL", "PENDIENTE", v);
        assertThat(adaptador.buscar(p.getId()).orElseThrow().articulos().get(0).aceptaMsi()).isFalse();
    }

    @Test
    void traduceLosTiposDePedidoYLosEstados() {
        Variantes v = articulo(false);
        assertThat(adaptador.buscar(pedido("APARTADO", "APARTADO", v).getId()).orElseThrow().tipo()).isEqualTo(TipoDeCobro.APARTADO);
        assertThat(adaptador.buscar(pedido("FIADO", "FIADO", v).getId()).orElseThrow().tipo()).isEqualTo(TipoDeCobro.IR_PAGANDO);
        assertThat(adaptador.buscar(pedido("NORMAL", "cancelado", v).getId()).orElseThrow().cancelado()).isTrue();
        assertThat(adaptador.buscar(pedido("NORMAL", "PAGADO", v).getId()).orElseThrow().pagado()).isTrue();
        assertThat(adaptador.buscar(pedido("NORMAL", "Entregado", v).getId()).orElseThrow().pagado()).isTrue();
        assertThat(adaptador.buscar(99999)).isEmpty();
    }

    @Test
    void marcarPagadoEnLaTerminal_dejaPagadoYEntregadoYCreaLaVentaUnaSolaVez() {
        Pedido p = pedido("NORMAL", "PENDIENTE", articulo(false));
        IntentoDePago i = intento(p.getId(), CanalDePago.TERMINAL, 1);

        assertThat(adaptador.marcarPagado(p.getId(), i.id())).isEqualTo(ResultadoAplicacion.APLICADO);
        assertThat(adaptador.marcarPagado(p.getId(), i.id())).isEqualTo(ResultadoAplicacion.NO_APLICABLE);   // ya pagado: no se duplica
        em.flush();
        em.clear();

        Pedido leido = pedidos.findById(p.getId()).orElseThrow();
        // un contado cobrado se guarda como 'Entregado' en estado_pedido (el PAGO) y su total_pagado se queda en 0,
        // como la venta de mostrador: es lo que esperan grupos, entregas y reportes
        assertThat(leido.getEstadoPedido()).isEqualTo("Entregado");
        assertThat(leido.getTotalPagado()).isEqualTo(0.0);
        assertThat(leido.isEntregado()).isTrue();
        var venta = ventas.findByPedidoId(p.getId()).orElseThrow();
        assertThat(venta.getTotalVenta()).isEqualTo(450.0);
        assertThat(venta.getDetalles()).hasSize(1);
        assertThat(venta.getDetalles().get(0).getGanancia()).isEqualTo(350.0);
    }

    @Test
    void aMesesSinIntereses_laVentaUsaLaFormaDePagoDeCreditoConEsosMeses() {
        Pedido p = pedido("NORMAL", "PENDIENTE", articulo(true));
        IntentoDePago i = intento(p.getId(), CanalDePago.TERMINAL, 3);
        adaptador.marcarPagado(p.getId(), i.id());
        em.flush();
        em.clear();
        var venta = ventas.findByPedidoId(p.getId()).orElseThrow();
        assertThat(venta.getPagosYMeses().getMesesIntereses().getMeses()).isEqualTo("3");
        assertThat(venta.getPagosYMeses().getTipoPago().getFormaPago()).isEqualTo("Tarjeta credito");
    }

    @Test
    void siNoHayFilaDeEsosMeses_seRegistraComoTarjetaDeContado() {
        Pedido p = pedido("NORMAL", "PENDIENTE", articulo(true));
        IntentoDePago i = intento(p.getId(), CanalDePago.TERMINAL, 6);
        adaptador.marcarPagado(p.getId(), i.id());
        em.flush();
        em.clear();
        assertThat(ventas.findByPedidoId(p.getId()).orElseThrow().getPagosYMeses().getMesesIntereses().getMeses()).isEqualTo("1");
    }

    @Test
    void marcarPagadoEnLinea_dejaPagadoPeroFaltaEntregar() {
        Pedido p = pedido("NORMAL", "PENDIENTE", articulo(false));
        IntentoDePago i = intento(p.getId(), CanalDePago.EN_LINEA, 1);
        adaptador.marcarPagado(p.getId(), i.id());
        em.flush();
        em.clear();
        Pedido leido = pedidos.findById(p.getId()).orElseThrow();
        assertThat(leido.getEstadoPedido()).isEqualTo("Entregado");
        assertThat(leido.isEntregado()).isFalse();                       // Pagado, falta entregar
        assertThat(ventas.findByPedidoId(p.getId())).isPresent();
    }

    @Test
    void despuesDePagarse_elPedidoSabeQuienLoPago() {
        Pedido p = pedido("NORMAL", "PENDIENTE", articulo(false));
        IntentoDePago i = intento(p.getId(), CanalDePago.TERMINAL, 1);
        adaptador.marcarPagado(p.getId(), i.id());
        em.flush();
        em.clear();
        // el intento tiene que estar aprobado para contar como "el que pago"
        assertThat(adaptador.buscar(p.getId()).orElseThrow().pagadoPorIntentoId()).isNull();
        intentos.guardar(intentos.buscar(i.id()).orElseThrow().enviado("e", Instant.now()).aprobado(Instant.now()));
        em.flush();
        em.clear();
        assertThat(adaptador.buscar(p.getId()).orElseThrow().pagadoPorIntentoId()).isEqualTo(i.id());
    }

    // ── Hallazgos del revisor: el pedido se revisa de nuevo, ya bloqueado, antes de marcarlo ──

    private ResultadoAplicacion aplicarA(Pedido p) {
        IntentoDePago i = intento(p.getId(), CanalDePago.TERMINAL, 1);
        ResultadoAplicacion r = adaptador.marcarPagado(p.getId(), i.id());
        em.flush();
        em.clear();
        return r;
    }

    @Test
    void unPedidoCanceladoNoSeRevive() {
        Pedido p = pedido("NORMAL", "cancelado", articulo(false));
        assertThat(aplicarA(p)).isEqualTo(ResultadoAplicacion.NO_APLICABLE);
        assertThat(pedidos.findById(p.getId()).orElseThrow().getEstadoPedido()).isEqualTo("cancelado");
        assertThat(ventas.findByPedidoId(p.getId())).isEmpty();
    }

    @Test
    void unPedidoQueCambioAIrPagandoOYaTieneAbonosNoRecibeElPago() {
        Variantes v = articulo(false);
        Pedido fiado = pedido("FIADO", "FIADO", v);
        assertThat(aplicarA(fiado)).isEqualTo(ResultadoAplicacion.NO_APLICABLE);

        Pedido conAbono = pedido("NORMAL", "PENDIENTE", v);
        conAbono.setTotalPagado(100.0);
        pedidos.save(conAbono);
        em.flush();
        em.clear();
        assertThat(aplicarA(conAbono)).isEqualTo(ResultadoAplicacion.NO_APLICABLE);
        assertThat(ventas.findByPedidoId(conAbono.getId())).isEmpty();
    }

    @Test
    void siElTotalDelPedidoCambioNoSeMarcaPagado() {
        Pedido p = pedido("NORMAL", "PENDIENTE", articulo(false));
        IntentoDePago i = intento(p.getId(), CanalDePago.TERMINAL, 1);          // cobro de 450.00
        Pedido editado = pedidos.findById(p.getId()).orElseThrow();
        editado.setTotalPedido(600.0);                                          // se agrego un articulo despues
        pedidos.save(editado);
        em.flush();
        em.clear();
        assertThat(adaptador.marcarPagado(p.getId(), i.id())).isEqualTo(ResultadoAplicacion.NO_APLICABLE);
    }

    @Test
    void sinElUsuarioQueCobro_avisaConUnaExcepcionDeDatosYNoDejaNadaAMedias() {
        Pedido p = pedido("NORMAL", "PENDIENTE", articulo(false));
        IntentoDePago i = intentos.guardar(IntentoDePago.nuevo(p.getId(), null, CanalDePago.TERMINAL,
                new BigDecimal("450.00"), 1, "nadie", Instant.now()));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adaptador.marcarPagado(p.getId(), i.id()))
                .isInstanceOf(com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException.NoSePudoAplicar.class);
    }

    @Test
    void marcaSiElPedidoEstaEnUnGrupoActivo() {
        Pedido p = pedido("NORMAL", "PENDIENTE", articulo(false));
        assertThat(adaptador.buscar(p.getId()).orElseThrow().enGrupo()).isFalse();
    }
}
