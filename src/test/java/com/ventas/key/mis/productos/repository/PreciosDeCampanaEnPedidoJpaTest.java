package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.campana.infraestructura.salida.persistencia.PreciosDeCampanaEnPedidoJpaAdapter;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio;
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
 * Pasar un pedido a Ir pagando regresa los juguetes con precio de campaña a su precio de antes y el total sube; lo demás no se toca. El grupo
 * de precio por cantidad no deshace un precio de campaña al editar.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({PreciosDeCampanaEnPedidoJpaAdapter.class, RepreciarPorCantidadAdapter.class, GruposDePrecioJpaAdapter.class, PreciosBaseDeLineaJpaAdapter.class, TablasDisponibles.class, PrecioCantidadBeans.class})
class PreciosDeCampanaEnPedidoJpaTest {

    @Autowired private PreciosDeCampanaEnPedidoJpaAdapter campana;
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
    void restaurar_regresaLosJuguetesDeCampanaAlPrecioDeAntes_yRecalculaElTotal() {
        Producto juguete = producto("Carro", 250), bolsa = producto("Bolsa", 500);
        Pedido pedido = pedidoVacio();
        DetallePedido lj = linea(pedido, articulo(juguete, "U"), 2, 200);    // con precio de campana
        DetallePedido lb = linea(pedido, articulo(bolsa, "U"), 1, 500);
        pedido.setTotalPedido(900.0);
        preciosBase.guardar(java.util.Map.of(lj.getId(), new BigDecimal("250")), OrigenDePrecio.CAMPANA);
        em.flush();

        assertThat(campana.tiene(pedido.getId())).isTrue();
        assertThat(campana.restaurar(pedido.getId())).isTrue();
        em.flush();
        em.clear();

        assertThat(precioDe(lj)).isEqualTo(250.0);
        assertThat(precioDe(lb)).isEqualTo(500.0);
        Pedido releido = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertThat(releido.getTotalPedido()).isEqualTo(1000.0);
        assertThat(campana.tiene(pedido.getId())).as("la marca se borra").isFalse();
        assertThat(campana.restaurar(pedido.getId())).as("una segunda vez no hace nada").isFalse();
    }

    @Test
    void sinPreciosDeCampana_noCambiaNada() {
        Producto a = producto("A", 250);
        Pedido pedido = pedidoVacio();
        DetallePedido la = linea(pedido, articulo(a, "U"), 1, 250);
        pedido.setTotalPedido(250.0);
        assertThat(campana.tiene(pedido.getId())).isFalse();
        assertThat(campana.restaurar(pedido.getId())).isFalse();
        assertThat(precioDe(la)).isEqualTo(250.0);
    }

    @Test
    void elGrupoNoDeshaceUnPrecioDeCampanaAlEditar() {
        Producto a = producto("A", 250), b = producto("B", 300), c = producto("C", 180);
        grupos.guardar(new GrupoPrecioCantidad(null, "Juguetes", 3, null, null, true, List.of(
                new MiembroDeGrupo(a.getId(), null, new BigDecimal("230")),
                new MiembroDeGrupo(b.getId(), null, new BigDecimal("280")),
                new MiembroDeGrupo(c.getId(), null, new BigDecimal("160")))));
        Pedido pedido = pedidoVacio();
        DetallePedido la = linea(pedido, articulo(a, "U"), 1, 200);       // campana: 250 -> 200
        linea(pedido, articulo(b, "U"), 1, 300);
        linea(pedido, articulo(c, "U"), 1, 180);
        preciosBase.guardar(java.util.Map.of(la.getId(), new BigDecimal("250")), OrigenDePrecio.CAMPANA);
        em.flush();

        repreciar.repreciar(pedido.getId());     // juntan 3 piezas del grupo: B y C bajan, A (campana, $200 < $230) se queda
        em.flush();

        assertThat(precioDe(la)).as("el precio de campana no se toca").isEqualTo(200.0);
        assertThat(preciosBase.leer(List.of(la.getId()), OrigenDePrecio.CAMPANA)).containsKey(la.getId());
        assertThat(preciosBase.leer(List.of(la.getId()), OrigenDePrecio.GRUPO)).as("no se vuelve una marca de grupo").isEmpty();
    }
}
