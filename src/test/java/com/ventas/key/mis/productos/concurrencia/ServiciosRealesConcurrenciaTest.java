package com.ventas.key.mis.productos.concurrencia;

import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.models.abonos.CancelarAbonoRequest;
import com.ventas.key.mis.productos.models.DetalleVentaDto;
import com.ventas.key.mis.productos.models.VentaDirectaRequest;
import com.ventas.key.mis.productos.models.pedidos.ClienteDTOPedido;
import com.ventas.key.mis.productos.models.pedidos.DetallePedidosDTOPedido;
import com.ventas.key.mis.productos.models.pedidos.PedidosDTOPedido;
import com.ventas.key.mis.productos.repository.*;
import com.ventas.key.mis.productos.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

/**
 * Revision C (2026-11-05): ejercita los SERVICIOS REALES (proxy @Transactional de Spring, repositorios reales, H2 o MySQL)
 * con dos hilos. La unica "trampa" es una pausa de 1.5 s en el primer aviso a cache/restock (mocks) para que la primera
 * transaccion siga abierta mientras corre la segunda: es la ventana normal de una venta, solo que alargada.
 */
@SpringBootTest(classes = com.ventas.key.mis.productos.MisProductosApplication.class)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {"api.cors_angular=http://x", "api.imagenes=http://localhost:1", "clave-seguridad.clave=falsa", "guardar-imagenes.ruta_imagenes=/tmp", "mercadopago.access-token=falso", "mercadopago.device-id=falso", "openai.api-key=falsa", "pagos.tarjeta.checkout.front-url=http://x", "pagos.tarjeta.checkout.notificacion-url=http://x", "redis.dns=localhost", "seguridad.cors.origenes-permitidos=http://x", "spring.mail.username=x@x.com", "token.jwt=c2VjcmV0by1kZS1wcnVlYmEtc2VjcmV0by1kZS1wcnVlYmEtc2VjcmV0by1kZS1wcnVlYmE="})
class ServiciosRealesConcurrenciaTest {

    @MockBean private CacheService cacheService;
    @MockBean private RabbitTemplate rabbitTemplate;
    @MockBean private EmailService emailService;
    @MockBean private RestockNotificacionService restock;
    @MockBean(name = "cacheManager") private org.springframework.data.redis.cache.RedisCacheManager cacheManager;

    @Autowired private VentaServiceImpl ventas;
    @Autowired private PedidoServiceImpl pedidos;
    @Autowired private AbonoServiceImpl abonos;
    @Autowired private IProductosRepository productos;
    @Autowired private IVarianteRepository variantes;
    @Autowired private ICodigoBarrasRepository codigos;
    @Autowired private IUsuarioRepository usuarios;
    @Autowired private IClienteRepository clientes;
    @Autowired private JdbcTemplate jdbc;

    private final AtomicInteger llamadasHook = new AtomicInteger();
    private volatile CountDownLatch primeroDentro;
    private Usuario usuario;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        org.mockito.Mockito.when(cacheManager.getCache(org.mockito.ArgumentMatchers.anyString())).thenReturn(org.mockito.Mockito.mock(org.springframework.cache.Cache.class));
        llamadasHook.set(0);
        primeroDentro = new CountDownLatch(1);
        Runnable pausa = () -> {
            if (llamadasHook.incrementAndGet() == 1) {
                primeroDentro.countDown();
                try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        };
        doAnswer(inv -> { pausa.run(); return null; }).when(cacheService).evictAll();
        doAnswer(inv -> { pausa.run(); return null; }).when(restock).notificarSiRestock(any(), anyInt());

        Usuario u = new Usuario();
        u.setUsername("caja" + System.nanoTime());
        u.setPassword("x");
        usuario = usuarios.save(u);
        Cliente c = new Cliente();
        c.setNombrePersona("Ana");
        c.setApeidoPaterno("Lopez");
        c.setNumeroTelefonico("5512345678");
        c.setCorreoElectronico("ana" + System.nanoTime() + "@x.com");
        c.setCorreoVerificado(true);
        c.setRecibirCorreos(false);
        cliente = clientes.save(c);
    }

    private Producto nuevoProducto(int stock) {
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras("750" + System.nanoTime());
        cb = codigos.save(cb);
        Producto p = new Producto();
        p.setNombre("Prueba " + System.nanoTime());
        p.setStock(stock);
        p.setHabilitado('1');
        p.setPrecioCosto(10.0);
        p.setPrecioVenta(20.0);
        p.setPrecioRebaja(0.0);
        p.setPiezas(0.0);
        p.setCodigoBarras(cb);
        p.setEsCatalogoInterno(false);
        return productos.save(p);
    }

    private Variantes nuevoArticulo(Producto p, int stock) {
        Variantes v = new Variantes();
        v.setProducto(p);
        v.setStock(stock);
        return variantes.save(v);
    }

    private static void comoAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "x", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private VentaDirectaRequest ventaApartado(int varianteId) {
        VentaDirectaRequest r = new VentaDirectaRequest();
        r.setUsuarioId(usuario.getId());
        r.setClienteId(cliente.getId());
        r.setTipoPedido("APARTADO");
        r.setDetalles(List.of(new DetalleVentaDto(varianteId, 1, 20.0, 20.0, null)));
        return r;
    }

    private PedidosDTOPedido pedidoCliente(int varianteId) {
        PedidosDTOPedido r = new PedidosDTOPedido();
        ClienteDTOPedido c = new ClienteDTOPedido();
        c.setId(cliente.getId());
        r.setCliente(c);
        r.setEstadoPedido("Pendiente");
        r.setTipoPedido("NORMAL");
        r.setFechaPedido(LocalDate.now());
        DetallePedidosDTOPedido d = new DetallePedidosDTOPedido();
        d.setVarianteId(varianteId);
        d.setCantidad(1);
        d.setPrecioUnitario(20.0);
        r.setDetalles(List.of(d));
        return r;
    }

    /** Corre "primero"; cuando ya esta dentro de su transaccion (en la pausa) corre "segundo". Devuelve errores. */
    private List<String> enCarrera(Callable<?> primero, Callable<?> segundo) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(2);
        List<String> errores = new ArrayList<>();
        try {
            Future<?> f1 = ex.submit(() -> { comoAdmin(); return primero.call(); });
            if (!primeroDentro.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("el primero no llego a la pausa");
            Future<?> f2 = ex.submit(() -> { comoAdmin(); return segundo.call(); });
            for (Future<?> f : List.of(f1, f2)) {
                try { f.get(30, TimeUnit.SECONDS); } catch (ExecutionException e) { errores.add(String.valueOf(e.getCause())); }
            }
        } finally { ex.shutdownNow(); }
        return errores;
    }

    private int stockProducto(int id) { return jdbc.queryForObject("select stock from producto where id = ?", Integer.class, id); }
    private int stockArticulo(int id) { return jdbc.queryForObject("select stock from variantes where id = ?", Integer.class, id); }

    private void reporte(String caso, Producto p, Variantes a, Variantes b, List<String> errores, String esperado) {
        System.out.printf("### [%s] producto=%d articuloA=%d%s errores=%s | esperado: %s%n", caso, stockProducto(p.getId()),
                stockArticulo(a.getId()), b != null ? " articuloB=" + stockArticulo(b.getId()) : "", errores, esperado);
    }

    // ---------------- S1: VentaServiceImpl.saveVentaDetalle ----------------

    @Test @DisplayName("S1 venta directa: dos articulos distintos del mismo producto a la vez")
    void s1_venta_dos_articulos() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5), b = nuevoArticulo(p, 5);
        var err = enCarrera(() -> ventas.saveVentaDetalle(ventaApartado(a.getId())), () -> ventas.saveVentaDetalle(ventaApartado(b.getId())));
        reporte("S1 venta, articulos distintos", p, a, b, err, "producto=8 A=4 B=4");
    }

    @Test @DisplayName("S1 venta directa: el MISMO articulo dos veces a la vez")
    void s1_venta_mismo_articulo() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5);
        var err = enCarrera(() -> ventas.saveVentaDetalle(ventaApartado(a.getId())), () -> ventas.saveVentaDetalle(ventaApartado(a.getId())));
        reporte("S1 venta, mismo articulo", p, a, null, err, "producto=8 A=3");
    }

    // ---------------- S2: PedidoServiceImpl.savePedido ----------------

    @Test @DisplayName("S2 pedido: dos articulos distintos del mismo producto a la vez")
    void s2_pedido_dos_articulos() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5), b = nuevoArticulo(p, 5);
        var err = enCarrera(() -> pedidos.savePedido(pedidoCliente(a.getId()), null), () -> pedidos.savePedido(pedidoCliente(b.getId()), null));
        reporte("S2 pedido, articulos distintos", p, a, b, err, "producto=8 A=4 B=4");
    }

    @Test @DisplayName("S2 pedido: el MISMO articulo dos veces a la vez")
    void s2_pedido_mismo_articulo() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5);
        var err = enCarrera(() -> pedidos.savePedido(pedidoCliente(a.getId()), null), () -> pedidos.savePedido(pedidoCliente(a.getId()), null));
        reporte("S2 pedido, mismo articulo", p, a, null, err, "producto=8 A=3");
    }

    // ---------------- S4: cancelaciones ----------------

    private int pedidoCreado(Callable<Integer> crear) throws Exception {
        comoAdmin();
        try { return crear.call(); } finally { SecurityContextHolder.clearContext(); llamadasHook.set(0); primeroDentro = new CountDownLatch(1); }
    }

    @Test @DisplayName("S4 AbonoServiceImpl.cancelarPedido: el mismo APARTADO cancelado dos veces SEGUIDAS")
    void s4_abono_doble_cancelacion_seguida() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5);
        int id = pedidoCreado(() -> ventas.saveVentaDetalle(ventaApartado(a.getId())).getPedidoId());
        doAnswer(inv -> null).when(restock).notificarSiRestock(any(), anyInt());
        CancelarAbonoRequest r = new CancelarAbonoRequest();
        r.setMotivo("CANCELADO");
        abonos.cancelarPedido(id, r);
        String error = null;
        try { abonos.cancelarPedido(id, r); } catch (Exception e) { error = e.getMessage(); }
        System.out.printf("### [S4 abono, dos veces seguidas] producto=%d articulo=%d segundaLlamada=%s | esperado: producto=10 A=5 y error%n",
                stockProducto(p.getId()), stockArticulo(a.getId()), error);
    }

    @Test @DisplayName("S4 AbonoServiceImpl.cancelarPedido: el mismo APARTADO cancelado dos veces A LA VEZ")
    void s4_abono_doble_cancelacion_simultanea() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5);
        int id = pedidoCreado(() -> ventas.saveVentaDetalle(ventaApartado(a.getId())).getPedidoId());
        CancelarAbonoRequest r = new CancelarAbonoRequest();
        r.setMotivo("CANCELADO");
        var err = enCarrera(() -> abonos.cancelarPedido(id, r), () -> abonos.cancelarPedido(id, r));
        reporte("S4 abono, dos cancelaciones a la vez", p, a, null, err, "producto=10 A=5 y un error");
    }

    @Test @DisplayName("S4 cancelacion simultanea + una venta en medio: ¿se pierde la venta?")
    void s4_abono_cancelacion_pisa_una_venta() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5);
        int id = pedidoCreado(() -> ventas.saveVentaDetalle(ventaApartado(a.getId())).getPedidoId()); // 9 / 4
        CancelarAbonoRequest r = new CancelarAbonoRequest();
        r.setMotivo("CANCELADO");
        var err = enCarrera(() -> abonos.cancelarPedido(id, r), () -> ventas.saveVentaDetalle(ventaApartado(a.getId())));
        reporte("S4 abono cancelando + venta del mismo articulo", p, a, null, err, "producto=9 A=4");
    }

    @Test @DisplayName("S4 PedidoServiceImpl.deletePedidoById: el mismo pedido cancelado dos veces A LA VEZ")
    void s4_pedido_doble_cancelacion_simultanea() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5);
        int id = pedidoCreado(() -> pedidos.savePedido(pedidoCliente(a.getId()), null).getId());
        var err = enCarrera(() -> { pedidos.deletePedidoById(id, "CANCELADO"); return null; },
                () -> { pedidos.deletePedidoById(id, "CANCELADO"); return null; });
        reporte("S4 deletePedidoById, dos a la vez", p, a, null, err, "producto=10 A=5 y un error");
    }

    @Test @DisplayName("S4 deletePedidoById + una venta del mismo articulo en medio")
    void s4_pedido_cancelacion_pisa_una_venta() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5);
        int id = pedidoCreado(() -> pedidos.savePedido(pedidoCliente(a.getId()), null).getId()); // 9 / 4
        var err = enCarrera(() -> { pedidos.deletePedidoById(id, "CANCELADO"); return null; },
                () -> ventas.saveVentaDetalle(ventaApartado(a.getId())));
        reporte("S4 deletePedidoById + venta del mismo articulo", p, a, null, err, "producto=9 A=4");
    }
}
