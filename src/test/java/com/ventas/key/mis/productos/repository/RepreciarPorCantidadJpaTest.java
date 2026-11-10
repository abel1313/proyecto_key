package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.pedidoarticulo.infraestructura.salida.persistencia.RepreciarPorCantidadAdapter;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.MiembroDeGrupo;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.config.PrecioCantidadBeans;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.GruposDePrecioJpaAdapter;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.PreciosBaseDeLineaJpaAdapter;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fase 1b: al EDITAR un pedido ya guardado, el precio por cantidad se vuelve a decidir con lo que lleva el pedido en ese momento (G6), contra
 * una base real (H2) y con una sola sesión de JPA como en una petición real (se lee el pedido, se edita y se relee).
 *
 * <p>Los grupos de ejemplo: «Bolsas» mínimo 3, productos A ($250 → $200), B ($300 → $270) y C ($180 → $150).
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({RepreciarPorCantidadAdapter.class, GruposDePrecioJpaAdapter.class, PreciosBaseDeLineaJpaAdapter.class, TablasDisponibles.class, PrecioCantidadBeans.class})
class RepreciarPorCantidadJpaTest {

    @Autowired private RepreciarPorCantidadAdapter repreciar;
    @Autowired private GruposDePrecioJpaAdapter grupos;
    @Autowired private PreciosBaseDeLineaJpaAdapter preciosBase;
    @Autowired private IPedidoRepository pedidoRepository;
    @Autowired private IDetallePedidoRepository detalleRepository;
    @Autowired private IProductosRepository productosRepository;
    @Autowired private IVarianteRepository varianteRepository;
    @Autowired private ICodigoBarrasRepository codigoBarrasRepository;
    @Autowired private EntityManager em;

    private int consecutivo;

    private Producto producto(String nombre, double precio) {
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras("750000000" + (++consecutivo));
        cb = codigoBarrasRepository.save(cb);
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setStock(50);
        p.setHabilitado('1');
        p.setPrecioCosto(10.0);
        p.setPrecioVenta(precio);
        p.setPrecioRebaja(0.0);
        p.setPiezas(0.0);
        p.setCodigoBarras(cb);
        p.setEsCatalogoInterno(false);
        return productosRepository.save(p);
    }

    private Variantes articulo(Producto p, String talla) {
        Variantes v = new Variantes();
        v.setProducto(p);
        v.setTalla(talla);
        v.setStock(50);
        return varianteRepository.save(v);
    }

    private Pedido pedidoVacio() {
        Pedido p = new Pedido();
        p.setTipoPedido("APARTADO");
        p.setEstadoPedido("APARTADO");
        p.setTotalPedido(0.0);
        p.setTotalPagado(0.0);
        p.setFechaPedido(LocalDate.now());
        p.setFechaHoraRegistro(LocalDateTime.now());
        p.setDetalles(new java.util.ArrayList<>());
        return pedidoRepository.save(p);
    }

    private DetallePedido linea(Pedido pedido, Variantes v, int cantidad, double precio) {
        DetallePedido d = new DetallePedido();
        d.setPedido(pedido);
        d.setVariante(v);
        d.setProducto(v.getProducto());
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precio);
        d.setSubTotal(precio * cantidad);
        DetallePedido guardada = detalleRepository.save(d);
        pedido.getDetalles().add(guardada);   // como hace PedidoArticuloJpaAdapter: la lista en memoria se mantiene a mano
        return guardada;
    }

    private void grupoBolsas(Producto a, Producto b, Producto c) {
        grupos.guardar(new GrupoPrecioCantidad(null, "Bolsas", 3, null, null, true, List.of(
                new MiembroDeGrupo(a.getId(), null, new BigDecimal("200")),
                new MiembroDeGrupo(b.getId(), null, new BigDecimal("270")),
                new MiembroDeGrupo(c.getId(), null, new BigDecimal("150")))));
        em.flush();
    }

    private double precioDe(DetallePedido d) {
        return detalleRepository.findById(d.getId()).orElseThrow().getPrecioUnitario();
    }

    @Test
    void alAgregarLaTerceraPieza_lasTresLineasPasanAlPrecioDeGrupo_yRecuerdanElDeAntes() {
        Producto a = producto("A", 250), b = producto("B", 300), c = producto("C", 180);
        grupoBolsas(a, b, c);
        Pedido pedido = pedidoVacio();
        DetallePedido la = linea(pedido, articulo(a, "U"), 1, 250);
        DetallePedido lb = linea(pedido, articulo(b, "U"), 1, 300);
        // Con 2 piezas todavia no juntan el minimo: se queda todo normal.
        repreciar.repreciar(pedido.getId());
        assertThat(precioDe(la)).isEqualTo(250.0);
        assertThat(precioDe(lb)).isEqualTo(300.0);

        DetallePedido lc = linea(pedido, articulo(c, "U"), 1, 180);   // la tercera pieza
        repreciar.repreciar(pedido.getId());
        em.flush();

        assertThat(precioDe(la)).isEqualTo(200.0);
        assertThat(precioDe(lb)).isEqualTo(270.0);
        assertThat(precioDe(lc)).isEqualTo(150.0);
        assertThat(preciosBase.leer(List.of(la.getId(), lb.getId(), lc.getId()))).hasSize(3);
        assertThat(preciosBase.leer(List.of(la.getId())).get(la.getId())).isEqualByComparingTo("250");
    }

    @Test
    void alQuitarUnaPieza_loQueQuedaRegresaAlPrecioNormal_yLaMarcaSeBorra() {
        Producto a = producto("A", 250), b = producto("B", 300), c = producto("C", 180);
        grupoBolsas(a, b, c);
        Pedido pedido = pedidoVacio();
        DetallePedido la = linea(pedido, articulo(a, "U"), 1, 250);
        DetallePedido lb = linea(pedido, articulo(b, "U"), 1, 300);
        DetallePedido lc = linea(pedido, articulo(c, "U"), 1, 180);
        repreciar.repreciar(pedido.getId());
        em.flush();
        assertThat(precioDe(la)).isEqualTo(200.0);

        // El cliente ya no quiere C: se quita la linea (como borrarLineas del adaptador) y se vuelve a calcular.
        pedido.getDetalles().remove(lc);
        detalleRepository.delete(lc);
        em.flush();
        repreciar.repreciar(pedido.getId());
        em.flush();

        assertThat(precioDe(la)).as("ya no junta 3 piezas: vuelve al precio de antes").isEqualTo(250.0);
        assertThat(precioDe(lb)).isEqualTo(300.0);
        assertThat(preciosBase.leer(List.of(la.getId(), lb.getId()))).as("la marca se borra").isEmpty();
        double total = pedidoRepository.findById(pedido.getId()).orElseThrow().getDetalles().stream().mapToDouble(d -> d.getPrecioUnitario() * d.getCantidad()).sum();
        assertThat(total).isEqualTo(550.0);
    }

    @Test
    void unaLineaNuevaDelMismoArticuloAPrecioNormal_seFusionaConLaQueYaEstabaConPrecioDeGrupo() {
        Producto a = producto("A", 250), b = producto("B", 300), c = producto("C", 180);
        grupoBolsas(a, b, c);
        Pedido pedido = pedidoVacio();
        Variantes va = articulo(a, "U");
        linea(pedido, va, 1, 250);
        linea(pedido, articulo(b, "U"), 1, 300);
        linea(pedido, articulo(c, "U"), 1, 180);
        repreciar.repreciar(pedido.getId());
        em.flush();

        // Se agrega otra pieza de A: entra a precio normal, como hace el servicio (el de grupo lo pone repreciar).
        linea(pedido, va, 1, 250);
        repreciar.repreciar(pedido.getId());
        em.flush();
        em.clear();

        List<DetallePedido> lineasDeA = pedidoRepository.findById(pedido.getId()).orElseThrow().getDetalles().stream()
                .filter(d -> d.getVariante().getId().equals(va.getId())).toList();
        assertThat(lineasDeA).as("una sola linea del articulo").hasSize(1);
        assertThat(lineasDeA.get(0).getCantidad()).isEqualTo(2);
        assertThat(lineasDeA.get(0).getPrecioUnitario()).isEqualTo(200.0);
        assertThat(lineasDeA.get(0).getSubTotal()).isEqualTo(400.0);
    }

    @Test
    void sinGrupos_nadaCambia() {
        Producto a = producto("A", 250);
        Pedido pedido = pedidoVacio();
        DetallePedido la = linea(pedido, articulo(a, "U"), 3, 250);
        repreciar.repreciar(pedido.getId());
        assertThat(precioDe(la)).isEqualTo(250.0);
    }

    @Test
    void esPrecioDeGrupo_soloParaElPrecioVigenteDeSuProducto() {
        Producto a = producto("A", 250), b = producto("B", 300), c = producto("C", 180);
        grupoBolsas(a, b, c);
        Variantes va = articulo(a, "U");
        assertThat(repreciar.esPrecioDeGrupo(va.getId(), 200.0)).isTrue();
        assertThat(repreciar.esPrecioDeGrupo(va.getId(), 199.0)).isFalse();
        assertThat(repreciar.esPrecioDeGrupo(articulo(producto("D", 100), "U").getId(), 200.0)).as("un producto fuera de todo grupo").isFalse();
    }
}
