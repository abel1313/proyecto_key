package com.ventas.key.mis.productos.service;

import com.ventas.key.hexagonal.preciocantidad.aplicacion.servicio.CalcularPreciosPorCantidadService;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.MiembroDeGrupo;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.PrecioDeGrupo;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.models.DetalleVentaDto;
import com.ventas.key.mis.productos.models.pedidos.DetallePedidosDTOPedido;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * El precio por cantidad (grupos) en el cobro, para Venta directa y para el carrito (PedidoServiceImpl): se calcula con TODO lo que lleva la
 * venta, el precio de grupo solo vale si la venta junto el minimo, y gana el precio mas bajo. Las reglas del grupo ya estan probadas en
 * {@code ReglasPrecioCantidadTest}; aqui se prueba el cableado en los dos servicios (metodos privados, por reflexion, como
 * {@code PrecioCatalogoValidacionTest}).
 */
class PrecioPorCantidadEnCobroTest {

    private static final double NORMAL = 250.0;
    private static final double DE_GRUPO = 200.0;

    private VentaServiceImpl venta;
    private PedidoServiceImpl pedidos;
    private IVarianteRepository variantes;

    private static Producto producto(int id) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre("Bolsa " + id);
        p.setPrecioVenta(NORMAL);
        return p;
    }

    private Variantes variante(int id, Producto p) {
        Variantes v = new Variantes();
        v.setId(id);
        v.setProducto(p);
        when(variantes.findById(id)).thenReturn(Optional.of(v));
        return v;
    }

    private static PrecioDeGrupo grupo(int productoId, double precio) {
        return new PrecioDeGrupo(productoId, 1, "Bolsas", BigDecimal.valueOf(precio), 2, 2);
    }

    @org.junit.jupiter.api.AfterEach
    void limpiarSesion() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void preparar() {
        variantes = mock(IVarianteRepository.class);
        venta = Mockito.mock(VentaServiceImpl.class, Mockito.CALLS_REAL_METHODS);
        pedidos = Mockito.mock(PedidoServiceImpl.class, Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(venta, "iVarianteRepository", variantes);
        ReflectionTestUtils.setField(pedidos, "iVarianteRepository", variantes);
    }

    // ── Validacion del precio ──────────────────────────────────────────────────

    @Test
    @DisplayName("Venta directa: el precio de grupo se acepta solo si la venta junto el minimo")
    void ventaAceptaElPrecioDeGrupoSoloConGrupo() {
        Producto p = producto(1);
        Variantes v = variante(10, p);
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(venta, "validarPrecioCatalogo", p, v, DE_GRUPO, grupo(1, DE_GRUPO))).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(venta, "validarPrecioCatalogo", p, v, DE_GRUPO))
                .hasMessageContaining("no es valido");
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(venta, "validarPrecioCatalogo", p, v, NORMAL, grupo(1, DE_GRUPO))).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(venta, "validarPrecioCatalogo", p, v, 1.0, grupo(1, DE_GRUPO)))
                .hasMessageContaining("200.00 (precio de grupo)");
    }

    @Test
    @DisplayName("Carrito: el precio de grupo se acepta solo si el pedido junto el minimo")
    void carritoAceptaElPrecioDeGrupoSoloConGrupo() {
        // El carrito mira quien es (la rebaja solo la da el admin): se prueba como una clienta.
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("clienta", null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USUARIO"))));
        Producto p = producto(1);
        Variantes v = variante(10, p);
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(pedidos, "validarPrecioCatalogo", p, v, DE_GRUPO, grupo(1, DE_GRUPO))).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(pedidos, "validarPrecioCatalogo", p, v, DE_GRUPO))
                .hasMessageContaining("no es valido");
    }

    // ── Gana el mas bajo ───────────────────────────────────────────────────────

    @Test
    @DisplayName("se cobra el de grupo si es menor; si el que llego ya es menor (una rebaja), se queda; sin grupo, no cambia")
    void ganaElMasBajo() {
        assertThat((double) ReflectionTestUtils.invokeMethod(VentaServiceImpl.class, "precioAplicado", NORMAL, grupo(1, DE_GRUPO))).isEqualTo(DE_GRUPO);
        assertThat((double) ReflectionTestUtils.invokeMethod(VentaServiceImpl.class, "precioAplicado", 180.0, grupo(1, DE_GRUPO))).isEqualTo(180.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(VentaServiceImpl.class, "precioAplicado", NORMAL, (Object) null)).isEqualTo(NORMAL);
        assertThat((double) ReflectionTestUtils.invokeMethod(PedidoServiceImpl.class, "precioAplicado", NORMAL, grupo(1, DE_GRUPO))).isEqualTo(DE_GRUPO);
    }

    // ── Calculo con todo lo que lleva la venta ─────────────────────────────────

    private CalcularPreciosPorCantidadService calculoConGrupoDe3(int... productos) {
        var miembros = java.util.Arrays.stream(productos).mapToObj(id -> new MiembroDeGrupo(id, "Bolsa " + id, new BigDecimal("200"))).toList();
        var g = new GrupoPrecioCantidad(1, "Bolsas", 3, null, null, true, miembros);
        var puerto = mock(com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida.GruposDePrecioPort.class);
        when(puerto.activosConProductos(Mockito.anySet())).thenReturn(List.of(g));
        return new CalcularPreciosPorCantidadService(puerto, Clock.fixed(Instant.parse("2026-12-10T15:00:00Z"), ZoneId.of("UTC")));
    }

    @Test
    @DisplayName("Venta directa: 1 pieza de cada uno de 3 productos del grupo (con articulos distintos) juntan el minimo y entran los tres")
    void ventaSumaLasPiezasDeTodasLasLineas() {
        variante(11, producto(1)); variante(12, producto(2)); variante(13, producto(3));
        ReflectionTestUtils.setField(venta, "preciosPorCantidad", calculoConGrupoDe3(1, 2, 3));

        Map<Integer, PrecioDeGrupo> r = ReflectionTestUtils.invokeMethod(venta, "preciosDeGrupoDeLaVenta", List.of(linea(11, 1, null), linea(12, 1, null), linea(13, 1, null)));
        assertThat(r).containsOnlyKeys(1, 2, 3);

        Map<Integer, PrecioDeGrupo> pocas = ReflectionTestUtils.invokeMethod(venta, "preciosDeGrupoDeLaVenta", List.of(linea(11, 1, null), linea(12, 1, null)));
        assertThat(pocas).isEmpty();
    }

    @Test
    @DisplayName("Venta directa: las lineas de una promocion (combo) no cuentan para el grupo")
    void ventaIgnoraLasLineasDeUnCombo() {
        variante(11, producto(1)); variante(12, producto(2)); variante(13, producto(3));
        ReflectionTestUtils.setField(venta, "preciosPorCantidad", calculoConGrupoDe3(1, 2, 3));

        Map<Integer, PrecioDeGrupo> r = ReflectionTestUtils.invokeMethod(venta, "preciosDeGrupoDeLaVenta", List.of(linea(11, 1, null), linea(12, 1, null), linea(13, 1, 99)));
        assertThat(r).isEmpty();
    }

    @Test
    @DisplayName("sin el servicio de grupos todo se cobra como antes")
    void sinServicioNoHayPrecioDeGrupo() {
        variante(11, producto(1));
        Map<Integer, PrecioDeGrupo> r = ReflectionTestUtils.invokeMethod(venta, "preciosDeGrupoDeLaVenta", List.of(linea(11, 5, null)));
        assertThat(r).isEmpty();
    }

    @Test
    @DisplayName("Carrito: cuenta tambien un renglon que viene solo con el producto (sin articulo)")
    void carritoCuentaRenglonesSinArticulo() {
        variante(11, producto(1));
        ReflectionTestUtils.setField(pedidos, "preciosPorCantidad", calculoConGrupoDe3(1, 2));

        var porArticulo = new DetallePedidosDTOPedido();
        porArticulo.setVarianteId(11);
        porArticulo.setCantidad(2);
        var soloProducto = new DetallePedidosDTOPedido();
        var dto = new com.ventas.key.mis.productos.models.pedidos.ProductoDTOPedidos();
        dto.setId(2);
        soloProducto.setProducto(dto);
        soloProducto.setCantidad(1);

        Map<Integer, PrecioDeGrupo> r = ReflectionTestUtils.invokeMethod(pedidos, "preciosDeGrupoDelPedido", List.of(porArticulo, soloProducto));
        assertThat(r).containsOnlyKeys(1, 2);
        assertThat(r.get(1).piezasDelGrupo()).isEqualTo(3);
    }

    private static DetalleVentaDto linea(int varianteId, int cantidad, Integer promocionId) {
        DetalleVentaDto d = new DetalleVentaDto();
        d.setVarianteId(varianteId);
        d.setCantidad(cantidad);
        d.setPromocionId(promocionId);
        return d;
    }
}
