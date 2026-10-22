package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoIntento;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.CancelarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ConfirmarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.DevolverCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.IniciarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela.PasarelaSimuladaAdapter;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.MesesIntereses;
import com.ventas.key.mis.productos.entity.PagosYMeses;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.TipoPago;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.config.PagoTarjetaBeans;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.config.PagoTarjetaConfig;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia.DevolucionesJpaAdapter;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia.IntentosDePagoJpaAdapter;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia.PedidoCobroAdapter;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia.PlanesMsiJdbcAdapter;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia.TransaccionSpringAdapter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El cobro de punta a punta con TODO conectado como en QA (servicios, adaptadores reales, pasarela simulada, base H2) y
 * SIN transaccion de prueba, que es como corre el servidor: cada llamada confirma sus propios cambios.
 * Si el cableado de Spring, el mapeo de las tablas o las consultas nativas estuvieran mal, truena aqui.
 */
@DataJpaTest(properties = {"pagos.tarjeta.habilitado=true"})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({PagoTarjetaFlujoCompletoTest.PasarelaDePrueba.class, PagoTarjetaBeans.class, PagoTarjetaConfig.class,
        IntentosDePagoJpaAdapter.class, DevolucionesJpaAdapter.class, TransaccionSpringAdapter.class,
        PlanesMsiJdbcAdapter.class, PedidoCobroAdapter.class})
class PagoTarjetaFlujoCompletoTest {

    @TestConfiguration
    static class PasarelaDePrueba {
        /** Aprueba al instante: las reglas de centavos son las de siempre (.01 rechaza, .03 cancela). */
        @Bean
        PasarelaSimuladaAdapter pasarelaSimulada() {
            return new PasarelaSimuladaAdapter(Clock.systemUTC(), 0L);
        }
    }

    @Autowired private IniciarCobroCasoUso iniciar;
    @Autowired private ConfirmarCobroCasoUso confirmar;
    @Autowired private CancelarCobroCasoUso cancelar;
    @Autowired private DevolverCobroCasoUso devolver;
    @Autowired private IPedidoRepository pedidos;
    @Autowired private IDetallePedidoRepository detalles;
    @Autowired private IProductosRepository productos;
    @Autowired private IVarianteRepository variantes;
    @Autowired private ICodigoBarrasRepository codigos;
    @Autowired private IUsuarioRepository usuarios;
    @Autowired private IVentaRepository ventas;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private TransactionTemplate tx;

    private static boolean preparado = false;

    @BeforeAll
    static void marcar() {
        preparado = false;
    }

    private synchronized void prepararCatalogo() {
        if (preparado) return;
        jdbc.execute("ALTER TABLE producto ADD COLUMN IF NOT EXISTS acepta_msi TINYINT DEFAULT 0 NOT NULL");
        jdbc.execute("ALTER TABLE variantes ADD COLUMN IF NOT EXISTS acepta_msi TINYINT NULL");
        tx.executeWithoutResult(s -> {
            TipoPago tipo = new TipoPago();
            tipo.setFormaPago("Tarjeta Debito");
            em.persist(tipo);
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
            u.setUsername("admin-flujo");
            u.setPassword("x");
            em.persist(u);
        });
        preparado = true;
    }

    private Integer pedido(String tipo, String total) {
        prepararCatalogo();
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras("750" + System.nanoTime());
        cb = codigos.save(cb);
        Producto pr = new Producto();
        pr.setNombre("Bolsa");
        pr.setStock(10);
        pr.setHabilitado('1');
        pr.setPrecioCosto(100.0);
        pr.setPrecioVenta(Double.parseDouble(total));
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
        Pedido p = new Pedido();
        p.setTipoPedido(tipo);
        p.setEstadoPedido(tipo.equals("NORMAL") ? "PENDIENTE" : tipo);
        p.setTotalPedido(Double.parseDouble(total));
        p.setTotalPagado(0.0);
        p.setFechaPedido(LocalDate.now());
        p.setFechaHoraRegistro(LocalDateTime.now());
        p = pedidos.save(p);
        DetallePedido d = new DetallePedido();
        d.setPedido(p);
        d.setProducto(pr);
        d.setVariante(v);
        d.setCantidad(1);
        d.setPrecioUnitario(Double.parseDouble(total));
        d.setSubTotal(Double.parseDouble(total));
        detalles.save(d);
        return p.getId();
    }

    @Test
    void cobroAprobadoEnLaTerminal_dejaElPedidoPagadoYEntregadoConSuVenta_yDespuesSeDevuelve() {
        Integer pedidoId = pedido("NORMAL", "450.00");
        IntentoDePago creado = iniciar.iniciar(pedidoId, CanalDePago.TERMINAL, 1, "admin-flujo");
        assertThat(creado.estado()).isEqualTo(EstadoIntento.ENVIADO);

        IntentoDePago resuelto = confirmar.confirmar(creado.id());
        assertThat(resuelto.estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(confirmar.confirmar(creado.id()).estado()).isEqualTo(EstadoIntento.APROBADO);   // idempotente

        Pedido p = pedidos.findById(pedidoId).orElseThrow();
        assertThat(p.getEstadoPedido()).isEqualTo("Entregado");
        assertThat(p.isEntregado()).isTrue();
        assertThat(ventas.findByPedidoId(pedidoId)).isPresent();

        // y no se puede cobrar otra vez
        assertThatThrownBy(() -> iniciar.iniciar(pedidoId, CanalDePago.TERMINAL, 1, "admin-flujo"))
                .hasMessageContaining("ya está pagado");

        IntentoDePago parcial = devolver.devolver(creado.id(), new BigDecimal("50"), "admin-flujo");
        assertThat(parcial.estado()).isEqualTo(EstadoIntento.DEVUELTO_PARCIAL);
        assertThat(devolver.devolver(creado.id(), null, "admin-flujo").estado()).isEqualTo(EstadoIntento.DEVUELTO);
    }

    @Test
    void cobroAprobadoEnLinea_dejaElPedidoPagadoPeroFaltaEntregar() {
        Integer pedidoId = pedido("NORMAL", "300.00");
        IntentoDePago i = iniciar.iniciar(pedidoId, CanalDePago.EN_LINEA, 1, "admin-flujo");
        assertThat(confirmar.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.APROBADO);
        Pedido p = pedidos.findById(pedidoId).orElseThrow();
        assertThat(p.getEstadoPedido()).isEqualTo("Entregado");
        assertThat(p.isEntregado()).isFalse();
    }

    @Test
    void losCentavosProvocanRechazoYCancelacion_yElPedidoQuedaSinPagar() {
        Integer rechazado = pedido("NORMAL", "450.01");
        IntentoDePago a = iniciar.iniciar(rechazado, CanalDePago.TERMINAL, 1, "admin-flujo");
        assertThat(confirmar.confirmar(a.id()).estado()).isEqualTo(EstadoIntento.RECHAZADO);
        assertThat(pedidos.findById(rechazado).orElseThrow().getEstadoPedido()).isEqualTo("PENDIENTE");

        Integer cancelado = pedido("NORMAL", "450.03");
        IntentoDePago b = iniciar.iniciar(cancelado, CanalDePago.TERMINAL, 1, "admin-flujo");
        assertThat(confirmar.confirmar(b.id()).estado()).isEqualTo(EstadoIntento.CANCELADO);
    }

    @Test
    void cancelarUnCobroEnProcesoLoDejaCancelado() {
        Integer pedidoId = pedido("NORMAL", "450.02");                         // .02: nunca contesta
        IntentoDePago i = iniciar.iniciar(pedidoId, CanalDePago.TERMINAL, 1, "admin-flujo");
        assertThat(confirmar.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.ENVIADO);
        assertThat(cancelar.cancelar(i.id()).estado()).isEqualTo(EstadoIntento.CANCELADO);
        assertThat(pedidos.findById(pedidoId).orElseThrow().getEstadoPedido()).isEqualTo("PENDIENTE");
    }

    @Test
    void apartadoEIrPagandoNoAceptanTarjeta_yMesesSinInteresesSinCasillaTampoco() {
        Integer apartado = pedido("APARTADO", "450.00");
        assertThatThrownBy(() -> iniciar.iniciar(apartado, CanalDePago.TERMINAL, 1, "admin-flujo"))
                .isInstanceOf(PagoConTarjetaException.class).hasMessageContaining("Apartado es sin dinero");
        Integer fiado = pedido("FIADO", "450.00");
        assertThatThrownBy(() -> iniciar.iniciar(fiado, CanalDePago.EN_LINEA, 1, "admin-flujo"))
                .hasMessageContaining("abonos en efectivo");
        Integer contado = pedido("NORMAL", "450.00");                          // el articulo no tiene la casilla de MSI
        assertThatThrownBy(() -> iniciar.iniciar(contado, CanalDePago.TERMINAL, 3, "admin-flujo"))
                .hasMessageContaining("no acepta meses sin intereses");
    }
}
