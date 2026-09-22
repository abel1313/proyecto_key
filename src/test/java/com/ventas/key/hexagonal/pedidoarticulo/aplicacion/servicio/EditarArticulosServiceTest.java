package com.ventas.key.hexagonal.pedidoarticulo.aplicacion.servicio;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.CambioRompePromocionException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.EdicionPedidoException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.PedidoCerradoException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.PedidoQuedariaVacioException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.PrecioNoCobrableException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.StockInsuficienteException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDePedido;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDisponible;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PedidoEditable;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PrecioCatalogo;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PromocionDelPedido;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.entrada.EditarArticulosCasoUso.AgregarArticulo;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.entrada.EditarArticulosCasoUso.CambiarArticulo;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.entrada.EditarArticulosCasoUso.ModoCambio;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.CatalogoArticuloPort;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.MovimientoStockPort;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.PedidoArticuloPort;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.PromocionDePedidoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Las reglas de editar los articulos de un pedido ya creado.
 *
 * <p>Se prueba contra puertos simulados a proposito: el dominio no depende de JPA, asi que no hace
 * falta una base para verificar que una promocion sale entera o que un precio inventado se
 * rechaza. Lo que los adaptadores hacen con esas ordenes es otra prueba.
 *
 * <p>El caso del negocio que da origen a todo esto (2026-09-22): pedido con pantalon de dama en
 * promocion, perfume y cartera; el cliente quiere el pantalon de hombre.
 */
class EditarArticulosServiceTest {

    private PedidoArticuloPort pedidos;
    private CatalogoArticuloPort catalogo;
    private MovimientoStockPort stock;
    private PromocionDePedidoPort promociones;
    private EditarArticulosService service;

    private static final int PEDIDO = 501;
    private static final int PROMO = 7;

    // Articulos del ejemplo
    private static final int PANTALON_DAMA = 10;
    private static final int PANTALON_HOMBRE = 20;
    private static final int PERFUME = 11;
    private static final int CARTERA = 12;

    @BeforeEach
    void setUp() {
        pedidos = mock(PedidoArticuloPort.class);
        catalogo = mock(CatalogoArticuloPort.class);
        stock = mock(MovimientoStockPort.class);
        promociones = mock(PromocionDePedidoPort.class);
        service = new EditarArticulosService(pedidos, catalogo, stock, promociones);
    }

    // ───────────────────────── armado del escenario ─────────────────────────

    private static ArticuloDePedido linea(int detalleId, int varianteId, int cantidad,
                                          double precio, Integer promocionId) {
        return new ArticuloDePedido(detalleId, varianteId, 99, "Articulo " + varianteId,
                cantidad, precio, promocionId);
    }

    /** El pedido del ejemplo: pantalon de dama en promocion, perfume en promocion, cartera suelta. */
    private PedidoEditable pedidoDelEjemplo() {
        PedidoEditable p = new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(
                linea(1, PANTALON_DAMA, 1, 300, PROMO),
                linea(2, PERFUME, 1, 200, PROMO),
                linea(3, CARTERA, 1, 500, null)));
        when(pedidos.buscarPedido(PEDIDO)).thenReturn(Optional.of(p));
        return p;
    }

    private void conPedido(PedidoEditable pedido) {
        when(pedidos.buscarPedido(PEDIDO)).thenReturn(Optional.of(pedido));
    }

    /** Un articulo del catalogo con stock de sobra. */
    private ArticuloDisponible enCatalogo(int varianteId, double normal, Double rebaja, int hay) {
        ArticuloDisponible a = new ArticuloDisponible(varianteId, 99, "Articulo " + varianteId,
                true, true, hay, hay, new PrecioCatalogo("Articulo " + varianteId, normal, rebaja));
        when(catalogo.leerParaEditar(varianteId)).thenReturn(Optional.of(a));
        return a;
    }

    /** La promocion del ejemplo: pantalon de DAMA y perfume. El de hombre NO esta. */
    private void conPromocionDeDamaYPerfume() {
        when(promociones.buscar(PROMO)).thenReturn(Optional.of(new PromocionDelPedido(
                PROMO, "Combo pantalon + perfume", true,
                Map.of(PANTALON_DAMA, 300.0, PERFUME, 200.0))));
    }

    // ───────────────────────────── Agregar ─────────────────────────────

    @Test
    @DisplayName("agregar un articulo descuenta su stock y crea la linea")
    void agregarCreaLinea() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(linea(3, CARTERA, 1, 500, null))));
        enCatalogo(PANTALON_HOMBRE, 400.0, 350.0, 5);

        service.agregar(PEDIDO, new AgregarArticulo(PANTALON_HOMBRE, 2, null));

        verify(stock).descontar(PANTALON_HOMBRE, 2);
        verify(pedidos).agregarLinea(PEDIDO, PANTALON_HOMBRE, 2, 400.0);
    }

    @Test
    @DisplayName("sin precio en el request se cobra el normal, nunca la rebaja")
    void sinPrecioVaElNormal() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(linea(3, CARTERA, 1, 500, null))));
        enCatalogo(PANTALON_HOMBRE, 400.0, 350.0, 5);

        service.agregar(PEDIDO, new AgregarArticulo(PANTALON_HOMBRE, 1, null));

        verify(pedidos).agregarLinea(PEDIDO, PANTALON_HOMBRE, 1, 400.0);
    }

    @Test
    @DisplayName("el precio de rebaja se puede cobrar explicitamente (R6)")
    void laRebajaSePuedeCobrar() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(linea(3, CARTERA, 1, 500, null))));
        enCatalogo(PANTALON_HOMBRE, 400.0, 350.0, 5);

        service.agregar(PEDIDO, new AgregarArticulo(PANTALON_HOMBRE, 1, 350.0));

        verify(pedidos).agregarLinea(PEDIDO, PANTALON_HOMBRE, 1, 350.0);
    }

    @Test
    @DisplayName("un precio inventado se rechaza y no mueve stock")
    void precioInventadoSeRechaza() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(linea(3, CARTERA, 1, 500, null))));
        enCatalogo(PANTALON_HOMBRE, 400.0, 350.0, 5);

        assertThatThrownBy(() -> service.agregar(PEDIDO, new AgregarArticulo(PANTALON_HOMBRE, 1, 1.0)))
                .isInstanceOf(PrecioNoCobrableException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("350");

        verify(stock, never()).descontar(anyInt(), anyInt());
    }

    @Test
    @DisplayName("agregar lo mismo que ya esta suma cantidad, no duplica la linea (R6)")
    void agregarLoMismoSumaCantidad() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0,
                List.of(linea(3, CARTERA, 1, 500, null))));
        enCatalogo(CARTERA, 500.0, null, 10);

        service.agregar(PEDIDO, new AgregarArticulo(CARTERA, 2, null));

        verify(pedidos).cambiarCantidad(3, 3);
        verify(pedidos, never()).agregarLinea(anyInt(), anyInt(), anyInt(), anyDouble());
    }

    @Test
    @DisplayName("agregar algo que solo esta como promocion crea una linea nueva (R5)")
    void noEngordaLaLineaDePromocion() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0,
                List.of(linea(1, PANTALON_DAMA, 1, 300, PROMO))));
        enCatalogo(PANTALON_DAMA, 450.0, null, 10);

        service.agregar(PEDIDO, new AgregarArticulo(PANTALON_DAMA, 1, null));

        // A precio normal (450) y en linea aparte: la de promocion vale 300 porque va con el combo.
        verify(pedidos).agregarLinea(PEDIDO, PANTALON_DAMA, 1, 450.0);
        verify(pedidos, never()).cambiarCantidad(anyInt(), anyInt());
    }

    @Test
    @DisplayName("sin stock no se agrega nada")
    void sinStockNoSeAgrega() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(linea(3, CARTERA, 1, 500, null))));
        enCatalogo(PANTALON_HOMBRE, 400.0, null, 1);

        assertThatThrownBy(() -> service.agregar(PEDIDO, new AgregarArticulo(PANTALON_HOMBRE, 5, null)))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Solicitado: 5");

        verify(stock, never()).descontar(anyInt(), anyInt());
    }

    @Test
    @DisplayName("un articulo dado de baja no se puede agregar")
    void articuloDeBajaNo() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(linea(3, CARTERA, 1, 500, null))));
        when(catalogo.leerParaEditar(PANTALON_HOMBRE)).thenReturn(Optional.of(
                new ArticuloDisponible(PANTALON_HOMBRE, 99, "Pantalon hombre", true, false, 10, 10,
                        new PrecioCatalogo("Pantalon hombre", 400.0, null))));

        assertThatThrownBy(() -> service.agregar(PEDIDO, new AgregarArticulo(PANTALON_HOMBRE, 1, null)))
                .isInstanceOf(EdicionPedidoException.class)
                .hasMessageContaining("dado de baja");
    }

    @Test
    @DisplayName("un articulo cuyo producto esta deshabilitado no se puede agregar, aunque el articulo este habilitado")
    void productoDeshabilitadoNo() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(linea(3, CARTERA, 1, 500, null))));
        when(catalogo.leerParaEditar(PANTALON_HOMBRE)).thenReturn(Optional.of(
                new ArticuloDisponible(PANTALON_HOMBRE, 99, "Pantalon hombre", false, true, 10, 10,
                        new PrecioCatalogo("Pantalon hombre", 400.0, null))));

        assertThatThrownBy(() -> service.agregar(PEDIDO, new AgregarArticulo(PANTALON_HOMBRE, 1, null)))
                .isInstanceOf(EdicionPedidoException.class)
                .hasMessageContaining("el producto está deshabilitado");

        verify(stock, never()).descontar(anyInt(), anyInt());
    }

    @Test
    @DisplayName("cantidad 0 o negativa se rechaza")
    void cantidadInvalida() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(linea(3, CARTERA, 1, 500, null))));

        assertThatThrownBy(() -> service.agregar(PEDIDO, new AgregarArticulo(PANTALON_HOMBRE, 0, null)))
                .isInstanceOf(EdicionPedidoException.class)
                .hasMessageContaining("mayor a 0");
    }

    @Test
    @DisplayName("un pedido entregado o cancelado no se edita (R1)")
    void pedidoCerradoNoSeEdita() {
        conPedido(new PedidoEditable(PEDIDO, "Entregado", 0.0, List.of(linea(3, CARTERA, 1, 500, null))));

        assertThatThrownBy(() -> service.agregar(PEDIDO, new AgregarArticulo(CARTERA, 1, null)))
                .isInstanceOf(PedidoCerradoException.class)
                .hasMessageContaining("ya se entrego");
    }

    // ───────────────────────────── Cambiar ─────────────────────────────

    @Test
    @DisplayName("cambiar una linea normal devuelve el stock viejo y descuenta el nuevo (R3)")
    void cambiarLineaNormal() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(linea(3, CARTERA, 2, 500, null))));
        enCatalogo(PANTALON_HOMBRE, 400.0, null, 10);

        service.cambiar(PEDIDO, 3, new CambiarArticulo(PANTALON_HOMBRE, null, null, ModoCambio.VALIDAR));

        verify(stock).devolver(CARTERA, 2);
        verify(stock).descontar(PANTALON_HOMBRE, 2);
        verify(pedidos).cambiarArticulo(3, PANTALON_HOMBRE, 400.0);
    }

    @Test
    @DisplayName("cambiar a la misma variante se rechaza en vez de no hacer nada")
    void cambiarALaMismaSeRechaza() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(linea(3, CARTERA, 1, 500, null))));

        assertThatThrownBy(() -> service.cambiar(PEDIDO, 3,
                new CambiarArticulo(CARTERA, null, null, ModoCambio.VALIDAR)))
                .isInstanceOf(EdicionPedidoException.class)
                .hasMessageContaining("ya es de ese mismo articulo");
    }

    @Test
    @DisplayName("el reemplazo que SI esta en el combo se cambia al precio del combo y no rompe nada")
    void cambioDentroDelCombo() {
        pedidoDelEjemplo();
        conPromocionDeDamaYPerfume();
        enCatalogo(PERFUME, 250.0, null, 10);

        // Cambiar la linea del pantalon de dama por el perfume, que si esta en el combo.
        service.cambiar(PEDIDO, 1, new CambiarArticulo(PERFUME, null, null, ModoCambio.VALIDAR));

        // Al precio del combo (200), no al de catalogo (250).
        verify(pedidos).cambiarArticulo(1, PERFUME, 200.0);
        verify(pedidos, never()).borrarLineas(any());
    }

    @Test
    @DisplayName("el caso real: el pantalon de hombre no esta en el combo -> se devuelven las 2 salidas")
    void rompeElComboYPregunta() {
        pedidoDelEjemplo();
        conPromocionDeDamaYPerfume();
        enCatalogo(PANTALON_HOMBRE, 400.0, null, 10);

        CambioRompePromocionException e = catchThrowableOfType(
                () -> service.cambiar(PEDIDO, 1,
                        new CambiarArticulo(PANTALON_HOMBRE, null, null, ModoCambio.VALIDAR)),
                CambioRompePromocionException.class);

        assertThat(e).isNotNull();

        // Las 2 lineas del combo, no solo la que se queria cambiar.
        assertThat(e.lineasDelCombo()).hasSize(2);
        assertThat(e.importeDelCombo()).isEqualTo(500.0);
        assertThat(e.promocion().descripcion()).isEqualTo("Combo pantalon + perfume");

        // Y sobre todo: no toco nada.
        verify(stock, never()).descontar(anyInt(), anyInt());
        verify(stock, never()).devolver(anyInt(), anyInt());
        verify(pedidos, never()).borrarLineas(any());
    }

    @Test
    @DisplayName("opcion (a): quitar la promocion saca el combo ENTERO y entra el nuevo a precio normal")
    void opcionQuitarPromocion() {
        pedidoDelEjemplo();
        conPromocionDeDamaYPerfume();
        enCatalogo(PANTALON_HOMBRE, 400.0, null, 10);

        service.cambiar(PEDIDO, 1,
                new CambiarArticulo(PANTALON_HOMBRE, null, null, ModoCambio.QUITAR_PROMOCION));

        // Las dos lineas del combo vuelven al inventario...
        verify(stock).devolver(PANTALON_DAMA, 1);
        verify(stock).devolver(PERFUME, 1);
        // ...la cartera NO se toca: es ajena a la promocion.
        verify(stock, never()).devolver(eq(CARTERA), anyInt());
        // ...y el nuevo entra a precio de catalogo, no al del combo.
        verify(pedidos).agregarLinea(PEDIDO, PANTALON_HOMBRE, 1, 400.0);
        verify(stock).descontar(PANTALON_HOMBRE, 1);
    }

    @Test
    @DisplayName("opcion (b): conservar la promocion no quita nada y suma el nuevo aparte")
    void opcionConservarPromocion() {
        pedidoDelEjemplo();
        conPromocionDeDamaYPerfume();
        enCatalogo(PANTALON_HOMBRE, 400.0, null, 10);

        service.cambiar(PEDIDO, 1,
                new CambiarArticulo(PANTALON_HOMBRE, null, null, ModoCambio.CONSERVAR_PROMOCION));

        verify(pedidos, never()).borrarLineas(any());
        verify(stock, never()).devolver(anyInt(), anyInt());
        verify(pedidos).agregarLinea(PEDIDO, PANTALON_HOMBRE, 1, 400.0);
    }

    @Test
    @DisplayName("si el nuevo no tiene stock, la promocion NO se desarma")
    void sinStockNoSeDesarmaElCombo() {
        pedidoDelEjemplo();
        conPromocionDeDamaYPerfume();
        enCatalogo(PANTALON_HOMBRE, 400.0, null, 0);

        assertThatThrownBy(() -> service.cambiar(PEDIDO, 1,
                new CambiarArticulo(PANTALON_HOMBRE, null, null, ModoCambio.QUITAR_PROMOCION)))
                .isInstanceOf(StockInsuficienteException.class);

        verify(pedidos, never()).borrarLineas(any());
        verify(stock, never()).devolver(anyInt(), anyInt());
    }

    @Test
    @DisplayName("cambiar una linea que no existe en el pedido")
    void lineaInexistente() {
        pedidoDelEjemplo();

        assertThatThrownBy(() -> service.cambiar(PEDIDO, 999,
                new CambiarArticulo(PANTALON_HOMBRE, null, null, ModoCambio.VALIDAR)))
                .hasMessageContaining("no existe en el pedido");
    }

    // ────────────────────── Quitar promocion ──────────────────────

    @Test
    @DisplayName("quitar una promocion saca todas sus lineas y devuelve su stock")
    void quitarPromocionCompleta() {
        pedidoDelEjemplo();

        service.quitarPromocion(PEDIDO, PROMO);

        verify(stock).devolver(PANTALON_DAMA, 1);
        verify(stock).devolver(PERFUME, 1);
        verify(stock, never()).devolver(eq(CARTERA), anyInt());
        verify(pedidos).borrarLineas(any());
    }

    @Test
    @DisplayName("quitar una promocion que el pedido no tiene")
    void promocionQueNoEsta() {
        pedidoDelEjemplo();

        assertThatThrownBy(() -> service.quitarPromocion(PEDIDO, 999))
                .isInstanceOf(EdicionPedidoException.class)
                .hasMessageContaining("no tiene articulos de la promocion");
    }

    @Test
    @DisplayName("no se puede vaciar un pedido quitandole la promocion: para eso esta cancelar")
    void noSePuedeVaciar() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 0.0, List.of(
                linea(1, PANTALON_DAMA, 1, 300, PROMO),
                linea(2, PERFUME, 1, 200, PROMO))));

        assertThatThrownBy(() -> service.quitarPromocion(PEDIDO, PROMO))
                .isInstanceOf(PedidoQuedariaVacioException.class)
                .hasMessageContaining("cancelar el pedido");

        verify(stock, never()).devolver(anyInt(), anyInt());
    }

    // ───────────────────────────── Total ─────────────────────────────

    @Test
    @DisplayName("el total se recalcula desde las lineas que quedaron (R7)")
    void elTotalSeRecalcula() {
        conPedido(new PedidoEditable(PEDIDO, "Pendiente", 100.0, List.of(
                linea(1, PANTALON_DAMA, 2, 300, null),
                linea(3, CARTERA, 1, 500, null))));
        enCatalogo(PANTALON_HOMBRE, 400.0, null, 10);

        service.agregar(PEDIDO, new AgregarArticulo(PANTALON_HOMBRE, 1, null));

        // 2x300 + 1x500 = 1100, leido del pedido releido (el mock devuelve el mismo).
        verify(pedidos).guardarTotal(PEDIDO, 1100.0);
    }

    @Test
    @DisplayName("editar un pedido con abonos no toca lo ya pagado (R7)")
    void noTocaLoPagado() {
        PedidoEditable conAbonos = new PedidoEditable(PEDIDO, "Pendiente", 800.0,
                List.of(linea(3, CARTERA, 1, 500, null)));
        conPedido(conAbonos);
        enCatalogo(CARTERA, 500.0, null, 10);

        assertThatCode(() -> service.agregar(PEDIDO, new AgregarArticulo(CARTERA, 1, null)))
                .doesNotThrowAnyException();

        // El saldo queda negativo (pago 800, ahora debe 500): es dinero a favor del cliente y
        // tiene que verse, no corregirse solo.
        assertThat(conAbonos.totalPagado()).isEqualTo(800.0);
    }
}
