package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia.PedidoCobroAdapter;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Como corren los trabajos programados: SIN transaccion ni sesion abierta. Con @DataJpaTest normal todo corre
 * dentro de una transaccion y la lectura perezosa de las lineas del pedido nunca falla; aqui si.
 * (Regresion del revisor: reconciliar() no aplicaba nunca un pago porque buscar() lanzaba LazyInitializationException.)
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(PedidoCobroAdapter.class)
class PedidoCobroAdapterSinTransaccionTest {

    @Autowired private PedidoCobroAdapter adaptador;
    @Autowired private IPedidoRepository pedidos;
    @Autowired private IDetallePedidoRepository detalles;
    @Autowired private IProductosRepository productos;
    @Autowired private IVarianteRepository variantes;
    @Autowired private ICodigoBarrasRepository codigos;

    @Test
    void buscarLeeLasLineasDelPedidoSinSesionAbierta() {
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
        Pedido p = new Pedido();
        p.setTipoPedido("NORMAL");
        p.setEstadoPedido("PENDIENTE");
        p.setTotalPedido(450.0);
        p.setTotalPagado(0.0);
        p.setFechaPedido(LocalDate.now());
        p.setFechaHoraRegistro(LocalDateTime.now());
        p = pedidos.save(p);
        DetallePedido d = new DetallePedido();
        d.setPedido(p);
        d.setProducto(pr);
        d.setVariante(v);
        d.setCantidad(1);
        d.setPrecioUnitario(450.0);
        d.setSubTotal(450.0);
        detalles.save(d);

        assertThat(adaptador.buscar(p.getId()).orElseThrow().articulos()).hasSize(1);
    }
}
