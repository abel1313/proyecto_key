package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.cortecaja.aplicacion.servicio.ArmarCorteDeCajaService;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.CorteDeCaja;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.CorteDeZona;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.FiltroDeCorte;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.PedidoPorCobrar;
import com.ventas.key.hexagonal.cortecaja.infraestructura.salida.persistencia.PedidosAbiertosJpaAdapter;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corte de caja contra H2 (2026-11-04): el adaptador lee los pedidos sin entregar y sin cancelar con saldo, con sus piezas, pagos y costo, y el
 * servicio los agrupa por zona. Es el ejemplo del dueño: Luvianos y Tejupilco, uno con anticipo y uno con descuento.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(PedidosAbiertosJpaAdapter.class)
class PedidosAbiertosJpaAdapterTest {

    @Autowired private PedidosAbiertosJpaAdapter adaptador;
    @Autowired private IPedidoRepository pedidos;
    @Autowired private IDetallePedidoRepository detalles;
    @Autowired private IProductosRepository productos;
    @Autowired private IVarianteRepository variantes;
    @Autowired private ICodigoBarrasRepository codigos;
    @Autowired private ILugarEntregaRepository lugares;
    @Autowired private EntityManager em;

    private LugarEntrega luvianos;
    private LugarEntrega tejupilco;
    /** Artículo de $100, costo $40 y descuento $80. */
    private Variantes conDescuento;
    /** Artículo de $100, costo $60, sin descuento. */
    private Variantes sinDescuento;

    @BeforeEach
    void preparar() {
        luvianos = zona("Luvianos");
        tejupilco = zona("Tejupilco");
        conDescuento = articulo("Termo", 40.0, 100.0, 80.0);
        sinDescuento = articulo("Playera", 60.0, 100.0, 0.0);
    }

    private LugarEntrega zona(String nombre) {
        LugarEntrega l = new LugarEntrega();
        l.setNombre(nombre);
        return lugares.save(l);
    }

    private Variantes articulo(String nombre, double costo, double venta, double rebaja) {
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras("750" + System.nanoTime());
        cb = codigos.save(cb);
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setStock(10);
        p.setHabilitado('1');
        p.setPrecioCosto(costo);
        p.setPrecioVenta(venta);
        p.setPrecioRebaja(rebaja);
        p.setPiezas(0.0);
        p.setCodigoBarras(cb);
        p.setEsCatalogoInterno(false);
        p = productos.save(p);
        Variantes v = new Variantes();
        v.setProducto(p);
        v.setStock(10);
        return variantes.save(v);
    }

    private Pedido pedido(LugarEntrega zona, String estado, double total, double pagado, boolean entregado, Variantes v, int cantidad) {
        Pedido p = new Pedido();
        p.setTipoPedido("NORMAL");
        p.setEstadoPedido(estado);
        p.setTotalPedido(total);
        p.setTotalPagado(pagado);
        p.setFechaPedido(LocalDate.of(2026, 11, 1));
        p.setFechaHoraRegistro(LocalDateTime.of(2026, 11, 1, 10, 0));
        p.setLugarEntrega(zona);
        p.setEntregado(entregado);
        p = pedidos.save(p);
        DetallePedido d = new DetallePedido();
        d.setPedido(p);
        d.setProducto(v.getProducto());
        d.setVariante(v);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(100.0);
        d.setSubTotal(100.0 * cantidad);
        detalles.save(d);
        return p;
    }

    private void abono(Pedido p, double monto) {
        em.createNativeQuery("INSERT INTO abono_pedido (pedido_id, monto, fecha_pago, metodo_pago) VALUES (?1, ?2, ?3, 'EFECTIVO')")
                .setParameter(1, p.getId()).setParameter(2, monto).setParameter(3, LocalDate.of(2026, 11, 2)).executeUpdate();
    }

    private CorteDeCaja corte() {
        return corte(FiltroDeCorte.porDefecto());
    }

    private CorteDeCaja corte(FiltroDeCorte filtro) {
        em.flush();
        em.clear();
        return new ArmarCorteDeCajaService(adaptador, Clock.systemUTC()).armar(filtro);
    }

    @Test
    @DisplayName("Luvianos y Tejupilco: solo los pedidos sin entregar, sin cancelar y con saldo; cada zona con su subtotal")
    void porZona() {
        pedido(luvianos, "Pendiente", 200.0, 0.0, false, conDescuento, 2);
        Pedido conAnticipo = pedido(luvianos, "APARTADO", 350.0, 100.0, false, sinDescuento, 1);
        abono(conAnticipo, 100.0);
        pedido(tejupilco, "Pendiente", 100.0, 0.0, false, sinDescuento, 1);
        pedido(tejupilco, "Pagado", 100.0, 100.0, false, sinDescuento, 1);   // ya pagado: no entra
        pedido(tejupilco, "Entregado", 100.0, 0.0, true, sinDescuento, 1);   // entregado: no entra
        pedido(tejupilco, "cancelado", 100.0, 0.0, false, sinDescuento, 1);  // cancelado: no entra
        pedido(null, "Pendiente", 50.0, 0.0, false, sinDescuento, 1);        // sin zona

        CorteDeCaja corte = corte();

        assertThat(corte.zonas()).extracting(CorteDeZona::zona).containsExactly("Luvianos", "Tejupilco", null);
        assertThat(corte.zonas().get(0).subtotal()).isEqualByComparingTo("450");   // 200 + (350 - 100)
        assertThat(corte.zonas().get(1).subtotal()).isEqualByComparingTo("100");
        assertThat(corte.zonas().get(2).subtotal()).isEqualByComparingTo("50");
        assertThat(corte.total()).isEqualByComparingTo("600");
        assertThat(corte.cantidadPedidos()).isEqualTo(4);
    }

    @Test
    @DisplayName("Piezas, pagos y anticipo de cada pedido")
    void piezasYPagos() {
        Pedido dos = pedido(luvianos, "Pendiente", 200.0, 0.0, false, conDescuento, 2);
        Pedido uno = pedido(luvianos, "APARTADO", 350.0, 100.0, false, sinDescuento, 1);
        abono(uno, 100.0);

        PedidoPorCobrar sinPagos = corte().zonas().get(0).pedidos().stream().filter(p -> p.pedidoId() == dos.getId()).findFirst().orElseThrow();
        PedidoPorCobrar conPago = corte().zonas().get(0).pedidos().stream().filter(p -> p.pedidoId() == uno.getId()).findFirst().orElseThrow();

        assertThat(sinPagos.articulos()).isEqualTo(2);
        assertThat(sinPagos.pagos()).isZero();
        assertThat(sinPagos.dioAnticipo()).isFalse();
        assertThat(conPago.articulos()).isEqualTo(1);
        assertThat(conPago.pagos()).isEqualTo(1);
        assertThat(conPago.dioAnticipo()).isTrue();
        assertThat(conPago.saldo()).isEqualByComparingTo("250");
    }

    @Test
    @DisplayName("Ganancia (admin): 2 termos de $100 con costo $40 y descuento $80 -> costo 80, normal 200 (ganancia 120), con descuento 160 (ganancia 80)")
    void ganancia() {
        Pedido p = pedido(luvianos, "Pendiente", 200.0, 0.0, false, conDescuento, 2);

        var montos = corte().zonas().get(0).pedidos().stream().filter(x -> x.pedidoId() == p.getId()).findFirst().orElseThrow().montos();

        assertThat(montos.costo()).isEqualByComparingTo("80");
        assertThat(montos.ventaNormal()).isEqualByComparingTo("200");
        assertThat(montos.gananciaNormal()).isEqualByComparingTo("120");
        assertThat(montos.conDescuento()).isEqualByComparingTo("160");
        assertThat(montos.gananciaConDescuento()).isEqualByComparingTo("80");
        assertThat(montos.hayDescuento()).isTrue();
    }

    @Test
    @DisplayName("Sin descuento: 'con descuento' es igual al precio normal y no se marca descuento")
    void sinDescuentoIgualaAlNormal() {
        Pedido p = pedido(luvianos, "Pendiente", 100.0, 0.0, false, sinDescuento, 1);

        var montos = corte().zonas().get(0).pedidos().stream().filter(x -> x.pedidoId() == p.getId()).findFirst().orElseThrow().montos();

        assertThat(montos.ventaNormal()).isEqualByComparingTo("100");
        assertThat(montos.conDescuento()).isEqualByComparingTo("100");
        assertThat(montos.hayDescuento()).isFalse();
        assertThat(montos.gananciaNormal()).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("La ganancia total del corte suma la de todas las zonas")
    void gananciaTotal() {
        pedido(luvianos, "Pendiente", 200.0, 0.0, false, conDescuento, 2);   // costo 80, normal 200
        pedido(tejupilco, "Pendiente", 100.0, 0.0, false, sinDescuento, 1);  // costo 60, normal 100

        var montos = corte().montos();

        assertThat(montos.costo()).isEqualByComparingTo("140");
        assertThat(montos.ventaNormal()).isEqualByComparingTo("300");
        assertThat(montos.gananciaNormal()).isEqualByComparingTo("160");
    }

    private Pedido pedidoDelDia(LugarEntrega zona, LocalDate dia, double total, double pagado, boolean entregado) {
        Pedido p = pedido(zona, "Pendiente", total, pagado, entregado, sinDescuento, 1);
        p.setFechaPedido(dia);
        return pedidos.save(p);
    }

    @Test
    @DisplayName("Filtro por zona: solo los pedidos de Luvianos")
    void filtroPorZona() {
        pedido(luvianos, "Pendiente", 100.0, 0.0, false, sinDescuento, 1);
        pedido(tejupilco, "Pendiente", 200.0, 0.0, false, sinDescuento, 1);
        CorteDeCaja corte = corte(new FiltroDeCorte(java.util.Set.of(luvianos.getId()), null, null, null, null));
        assertThat(corte.zonas()).extracting(CorteDeZona::zona).containsExactly("Luvianos");
        assertThat(corte.total()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("Filtro por fechas del pedido: ambos extremos incluidos")
    void filtroPorFechas() {
        pedidoDelDia(luvianos, LocalDate.of(2026, 10, 31), 10.0, 0.0, false);
        pedidoDelDia(luvianos, LocalDate.of(2026, 11, 1), 20.0, 0.0, false);
        pedidoDelDia(luvianos, LocalDate.of(2026, 11, 3), 40.0, 0.0, false);
        pedidoDelDia(luvianos, LocalDate.of(2026, 11, 4), 80.0, 0.0, false);

        CorteDeCaja entre = corte(new FiltroDeCorte(null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), null, null));
        assertThat(entre.total()).isEqualByComparingTo("60");   // 1 y 3 de noviembre

        CorteDeCaja desde = corte(new FiltroDeCorte(null, LocalDate.of(2026, 11, 3), null, null, null));
        assertThat(desde.total()).isEqualByComparingTo("120");

        CorteDeCaja hasta = corte(new FiltroDeCorte(null, null, LocalDate.of(2026, 10, 31), null, null));
        assertThat(hasta.total()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Pagados y por cobrar, en las mismas fechas y zona")
    void pagadosYPorCobrar() {
        pedido(luvianos, "Pagado", 100.0, 100.0, false, sinDescuento, 1);
        pedido(luvianos, "Pendiente", 50.0, 0.0, false, sinDescuento, 1);

        CorteDeCaja pagados = corte(new FiltroDeCorte(null, null, null, FiltroDeCorte.Pago.PAGADOS, FiltroDeCorte.Entrega.SIN_ENTREGAR));
        assertThat(pagados.cantidadPedidos()).isEqualTo(1);
        assertThat(pagados.totalPagado()).isEqualByComparingTo("100");

        CorteDeCaja todos = corte(new FiltroDeCorte(null, null, null, FiltroDeCorte.Pago.TODOS, FiltroDeCorte.Entrega.SIN_ENTREGAR));
        assertThat(todos.cantidadPedidos()).isEqualTo(2);
        assertThat(todos.totalPedidos()).isEqualByComparingTo("150");
        assertThat(todos.total()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("Entregados: con «Entregados» o «Todos» salen; con «Sin entregar» no. Los cancelados nunca")
    void filtroPorEntrega() {
        pedido(luvianos, "Pendiente", 100.0, 0.0, false, sinDescuento, 1);
        pedido(luvianos, "Entregado", 70.0, 70.0, true, sinDescuento, 1);
        pedido(luvianos, "cancelado", 999.0, 0.0, false, sinDescuento, 1);

        assertThat(corte(new FiltroDeCorte(null, null, null, FiltroDeCorte.Pago.TODOS, FiltroDeCorte.Entrega.SIN_ENTREGAR)).cantidadPedidos()).isEqualTo(1);
        assertThat(corte(new FiltroDeCorte(null, null, null, FiltroDeCorte.Pago.TODOS, FiltroDeCorte.Entrega.ENTREGADOS)).cantidadPedidos()).isEqualTo(1);
        assertThat(corte(new FiltroDeCorte(null, null, null, FiltroDeCorte.Pago.TODOS, FiltroDeCorte.Entrega.TODOS)).cantidadPedidos()).isEqualTo(2);
    }

    @Test
    @DisplayName("Varias zonas: Luvianos y Tejupilco juntas suman todo; con 0 se agrega «Sin zona»; sin tocar la otra zona")
    void filtroDeVariasZonas() {
        LugarEntrega zacazonapan = zona("Zacazonapan");
        pedido(luvianos, "Pendiente", 100.0, 0.0, false, sinDescuento, 1);
        pedido(tejupilco, "Pendiente", 200.0, 0.0, false, sinDescuento, 1);
        pedido(zacazonapan, "Pendiente", 400.0, 0.0, false, sinDescuento, 1);
        pedido(null, "Pendiente", 8.0, 0.0, false, sinDescuento, 1);

        CorteDeCaja dos = corte(new FiltroDeCorte(java.util.Set.of(luvianos.getId(), tejupilco.getId()), null, null, null, null));
        assertThat(dos.zonas()).extracting(CorteDeZona::zona).containsExactly("Luvianos", "Tejupilco");
        assertThat(dos.total()).isEqualByComparingTo("300");

        CorteDeCaja conSinZona = corte(new FiltroDeCorte(java.util.Set.of(tejupilco.getId(), FiltroDeCorte.SIN_ZONA), null, null, null, null));
        assertThat(conSinZona.zonas()).extracting(CorteDeZona::zona).containsExactly("Tejupilco", null);
        assertThat(conSinZona.total()).isEqualByComparingTo("208");

        CorteDeCaja soloSinZona = corte(new FiltroDeCorte(java.util.Set.of(FiltroDeCorte.SIN_ZONA), null, null, null, null));
        assertThat(soloSinZona.total()).isEqualByComparingTo("8");
    }

    @Test
    @DisplayName("Si el pedido cambia de zona o de pago, el siguiente corte lo refleja (se calcula con los datos de ese momento)")
    void reflejaLosCambios() {
        Pedido p = pedido(luvianos, "Pendiente", 100.0, 0.0, false, sinDescuento, 1);
        assertThat(corte(new FiltroDeCorte(java.util.Set.of(tejupilco.getId()), null, null, null, null)).cantidadPedidos()).isZero();

        Pedido cambiado = pedidos.findById(p.getId()).orElseThrow();
        cambiado.setLugarEntrega(tejupilco);
        cambiado.setTotalPagado(100.0);
        pedidos.save(cambiado);

        assertThat(corte(new FiltroDeCorte(java.util.Set.of(tejupilco.getId()), null, null, FiltroDeCorte.Pago.POR_COBRAR, null)).cantidadPedidos()).isZero(); // ya pagó
        assertThat(corte(new FiltroDeCorte(java.util.Set.of(tejupilco.getId()), null, null, FiltroDeCorte.Pago.PAGADOS, null)).cantidadPedidos()).isEqualTo(1);
        assertThat(corte(new FiltroDeCorte(java.util.Set.of(luvianos.getId()), null, null, FiltroDeCorte.Pago.TODOS, null)).cantidadPedidos()).isZero();
    }
}
