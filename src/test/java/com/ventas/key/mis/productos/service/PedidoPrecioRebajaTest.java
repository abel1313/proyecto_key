package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El precio con descuento es un descuento que el admin decide dar, no un precio de lista
 * (hotfix 2026-09-29): en un pedido solo lo puede cobrar el admin, y a un cliente ni el mensaje
 * de error se lo revela.
 */
class PedidoPrecioRebajaTest {

    private static final double NORMAL = 400.0;
    private static final double REBAJA = 350.0;

    // Sin constructor: la regla no usa ninguna dependencia del servicio.
    private final PedidoServiceImpl pedidos = Mockito.mock(PedidoServiceImpl.class, Mockito.CALLS_REAL_METHODS);

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setNombre("Great Jeans");
        producto.setPrecioVenta(NORMAL);
        producto.setPrecioRebaja(REBAJA);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void sesionCon(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "usuario", null, List.of(new SimpleGrantedAuthority(rol))));
    }

    private void cobrarA(Double precio) {
        cobrarA(precio, false);
    }

    private void cobrarA(Double precio, boolean usarDescuento) {
        Variantes articulo = new Variantes();
        articulo.setProducto(producto);
        articulo.setUsarDescuento(usarDescuento);
        ReflectionTestUtils.invokeMethod(pedidos, "validarPrecioCatalogo", producto, articulo, precio);
    }

    @Test
    @DisplayName("el admin puede cobrar el precio con descuento")
    void elAdminPuedeCobrarLaRebaja() {
        sesionCon("ROLE_ADMIN");
        assertThatCode(() -> cobrarA(REBAJA)).doesNotThrowAnyException();
        assertThatCode(() -> cobrarA(NORMAL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("un cliente no puede mandar el precio con descuento")
    void unClienteNoPuedeCobrarLaRebaja() {
        sesionCon("ROLE_USUARIO");
        assertThatThrownBy(() -> cobrarA(REBAJA))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no es valido");
    }

    @Test
    @DisplayName("a un cliente el mensaje de error no le dice el precio con descuento")
    void elMensajeNoLeRevelaLaRebajaAlCliente() {
        sesionCon("ROLE_USUARIO");
        assertThatThrownBy(() -> cobrarA(1.0))
                .hasMessageContaining("400")
                .hasMessageNotContaining("350")
                .hasMessageNotContaining("rebaja");
    }

    @Test
    @DisplayName("un cliente paga el precio normal sin problema")
    void elClientePagaElNormal() {
        sesionCon("ROLE_USUARIO");
        assertThatCode(() -> cobrarA(NORMAL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("R8: con el descuento activado en la card, el cliente paga el descuento")
    void conDescuentoActivoElClientePagaLaRebaja() {
        sesionCon("ROLE_USUARIO");
        assertThatCode(() -> cobrarA(REBAJA, true)).doesNotThrowAnyException();
    }
}
