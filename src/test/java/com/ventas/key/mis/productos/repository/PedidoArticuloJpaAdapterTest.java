package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDePedido;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PedidoEditable;
import com.ventas.key.hexagonal.pedidoarticulo.infraestructura.salida.persistencia.PedidoArticuloJpaAdapter;
import com.ventas.key.mis.productos.entity.AbonoPedido;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El adaptador de lineas de pedido contra una base real (H2), con <b>una sola sesion de JPA</b>
 * como en una peticion real: primero se lee el pedido (eso carga su lista de lineas en memoria),
 * despues se agrega o se borra una linea, y despues se vuelve a leer para sacar el total.
 *
 * <p>Regresion de QA 2026-10-01 (Prueba 4, caso 2): un pedido Ir pagando de $100 ya Pagado con
 * $150; se le agrega un articulo de $300 y el total se quedaba en $100 y el pedido seguia
 * "pagado por completo". La linea nueva si se guardaba, pero la lista de lineas del pedido que
 * ya estaba en memoria no la tenia, asi que el total se sacaba sin ella.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({PedidoArticuloJpaAdapter.class, AbonoServiceImpl.class})
class PedidoArticuloJpaAdapterTest {

    @Autowired private PedidoArticuloJpaAdapter adapter;
    @Autowired private AbonoServiceImpl abonoService;
    @Autowired private IPedidoRepository pedidoRepository;
    @Autowired private IDetallePedidoRepository detalleRepository;
    @Autowired private IProductosRepository productosRepository;
    @Autowired private IVarianteRepository varianteRepository;
    @Autowired private ICodigoBarrasRepository codigoBarrasRepository;
    @Autowired private IAbonoRepository abonoRepository;
    @Autowired private EntityManager em;

    @MockBean private EmailService emailService;
    @MockBean private WhatsappService whatsappService;
    @MockBean private RestockNotificacionService restockNotificacionService;

    private Producto producto;

    private Variantes articulo(String talla) {
        if (producto == null) {
            CodigoBarra cb = new CodigoBarra();
            cb.setCodigoBarras("7500000000001");
            cb = codigoBarrasRepository.save(cb);

            Producto p = new Producto();
            p.setNombre("Blusa");
            p.setStock(10);
            p.setHabilitado('1');
            p.setPrecioCosto(0.0);
            p.setPrecioVenta(100.0);
            p.setPrecioRebaja(0.0);
            p.setPiezas(0.0);
            p.setCodigoBarras(cb);
            p.setEsCatalogoInterno(false);
            producto = productosRepository.save(p);
        }
        Variantes v = new Variantes();
        v.setProducto(producto);
        v.setTalla(talla);
        v.setStock(10);
        return varianteRepository.save(v);
    }

    /** Pedido Ir pagando con una linea de $100 y $150 abonados: ya Pagado, como en QA. */
    private Pedido pedidoPagadoDeMas(Variantes v) {
        Pedido p = new Pedido();
        p.setTipoPedido("APARTADO");
        p.setEstadoPedido("PAGADO");
        p.setTotalPedido(100.0);
        p.setTotalPagado(150.0);
        p.setFechaPedido(LocalDate.now());
        p.setFechaHoraRegistro(LocalDateTime.now());
        p = pedidoRepository.save(p);

        linea(p, v, 100.0);

        AbonoPedido a = new AbonoPedido();
        a.setPedido(p);
        a.setMonto(150.0);
        a.setFechaPago(LocalDate.now());
        a.setMetodoPago("EFECTIVO");
        abonoRepository.save(a);
        return p;
    }

    private DetallePedido linea(Pedido p, Variantes v, double precio) {
        DetallePedido d = new DetallePedido();
        d.setPedido(p);
        d.setVariante(v);
        d.setProducto(v.getProducto());
        d.setCantidad(1);
        d.setPrecioUnitario(precio);
        d.setSubTotal(precio);
        return detalleRepository.save(d);
    }

    /** Lo que pasa entre una peticion y otra: lo escrito llega a la base y la memoria se vacia. */
    private void nuevaPeticion() {
        em.flush();
        em.clear();
    }

    @Test
    void agregar_un_articulo_entra_en_el_total_aunque_el_pedido_ya_se_hubiera_leido() {
        Variantes chica = articulo("CH");
        Variantes grande = articulo("G");
        Pedido p = pedidoPagadoDeMas(chica);
        nuevaPeticion();

        // Igual que EditarArticulosService.agregar(): valida leyendo, agrega, relee para el total.
        adapter.buscarPedido(p.getId()).orElseThrow();
        adapter.agregarLinea(p.getId(), grande.getId(), 1, 300.0);
        PedidoEditable releido = adapter.buscarPedido(p.getId()).orElseThrow();

        assertThat(releido.articulos()).hasSize(2);
        assertThat(releido.total()).isEqualTo(400.0);
    }

    @Test
    void el_pedido_pagado_que_recibe_un_articulo_vuelve_a_deber() {
        Variantes chica = articulo("CH");
        Variantes grande = articulo("G");
        Pedido p = pedidoPagadoDeMas(chica);
        nuevaPeticion();

        // El caso completo de QA: $100 pagado con $150, entra uno de $300 -> total $400, debe $250.
        adapter.buscarPedido(p.getId()).orElseThrow();
        adapter.agregarLinea(p.getId(), grande.getId(), 1, 300.0);
        adapter.guardarTotal(p.getId(), adapter.buscarPedido(p.getId()).orElseThrow().total());
        abonoService.ajustarTrasEditarArticulos(p.getId(), 1);
        nuevaPeticion();

        Pedido despues = pedidoRepository.findById(p.getId()).orElseThrow();
        assertThat(despues.getTotalPedido()).isEqualTo(400.0);
        assertThat(despues.getTotalPagado()).isEqualTo(150.0);
        assertThat(despues.getEstadoPedido()).isEqualTo("APARTADO");
    }

    @Test
    void borrar_una_linea_la_saca_del_total_y_de_la_base() {
        Variantes chica = articulo("CH");
        Variantes grande = articulo("G");
        Pedido p = pedidoPagadoDeMas(chica);
        DetallePedido sobra = linea(p, grande, 300.0);
        nuevaPeticion();

        PedidoEditable antes = adapter.buscarPedido(p.getId()).orElseThrow();
        ArticuloDePedido aBorrar = antes.linea(sobra.getId()).orElseThrow();
        adapter.borrarLineas(List.of(aBorrar));

        assertThat(adapter.buscarPedido(p.getId()).orElseThrow().total()).isEqualTo(100.0);
        nuevaPeticion();
        assertThat(detalleRepository.findById(sobra.getId())).isEmpty();
        assertThat(adapter.buscarPedido(p.getId()).orElseThrow().articulos()).hasSize(1);
    }
}
