package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Una linea sin promocion se cobra al precio normal o al de rebaja, y a ningun otro.
 *
 * <p>La regla vive en un metodo privado de los dos servicios de cobro; se llama por reflexion
 * porque llegar ahi por la puerta grande implicaria montar una venta entera con stock, cliente y
 * formas de pago, y eso probaria diez cosas a la vez.
 */
class PrecioCatalogoValidacionTest {

    private VentaServiceImpl venta;

    private static final double NORMAL = 400.0;
    private static final double REBAJA = 350.0;

    @BeforeEach
    void setUp() {
        venta = new VentaServiceImpl(
                mock(com.ventas.key.mis.productos.repository.IVentaRepository.class),
                mock(com.ventas.key.mis.productos.repository.IProductosRepository.class),
                mock(com.ventas.key.mis.productos.repository.IUsuarioRepository.class),
                mock(com.ventas.key.mis.productos.repository.IClienteRepository.class),
                mock(com.ventas.key.mis.productos.repository.IPagosYMesesRepository.class),
                mock(com.ventas.key.mis.productos.repository.IDetallePagoRepository.class),
                mock(com.ventas.key.mis.productos.repository.IVarianteRepository.class),
                mock(com.ventas.key.mis.productos.repository.IClienteSinRegistroRepository.class),
                mock(com.ventas.key.mis.productos.repository.IPedidoRepository.class),
                mock(com.ventas.key.mis.productos.repository.IPromocionRepository.class),
                mock(PromocionServiceImpl.class),
                mock(com.ventas.key.mis.productos.errores.ErrorGenerico.class));
    }

    private static Producto producto(Double normal, Double rebaja) {
        Producto p = new Producto();
        p.setId(279);
        p.setNombre("Great Jeans");
        p.setPrecioVenta(normal);
        p.setPrecioRebaja(rebaja);
        return p;
    }

    private void cobrarA(Producto prod, Double precio) {
        ReflectionTestUtils.invokeMethod(venta, "validarPrecioCatalogo", prod, precio);
    }

    @Test
    @DisplayName("el precio normal se acepta, como siempre")
    void elNormalSeAcepta() {
        assertThatCode(() -> cobrarA(producto(NORMAL, REBAJA), NORMAL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("el precio de rebaja ahora tambien se acepta")
    void laRebajaSeAcepta() {
        // Antes esto reventaba y obligaba a armar una promocion para poder cobrar mas barato.
        assertThatCode(() -> cobrarA(producto(NORMAL, REBAJA), REBAJA)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cualquier otro precio se sigue rechazando")
    void unPrecioInventadoSeRechaza() {
        assertThatThrownBy(() -> cobrarA(producto(NORMAL, REBAJA), 1.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no es valido");
    }

    @Test
    @DisplayName("el mensaje dice cuales son los precios validos")
    void elMensajeExplicaQuePrecioSiVale() {
        assertThatThrownBy(() -> cobrarA(producto(NORMAL, REBAJA), 1.0))
                .hasMessageContaining("400")
                .hasMessageContaining("350");
    }

    @Test
    @DisplayName("una rebaja en 0 significa 'sin rebaja', no 'gratis'")
    void rebajaEnCeroNoEsGratis() {
        assertThatThrownBy(() -> cobrarA(producto(NORMAL, 0.0), 0.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no es valido");
    }

    @Test
    @DisplayName("sin rebaja cargada, solo vale el normal")
    void sinRebajaSoloElNormal() {
        Producto sinRebaja = producto(NORMAL, null);

        assertThatCode(() -> cobrarA(sinRebaja, NORMAL)).doesNotThrowAnyException();
        assertThatThrownBy(() -> cobrarA(sinRebaja, REBAJA))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("una diferencia de centavos por redondeo no rompe el cobro")
    void toleraElRedondeoDeCentavos() {
        assertThatCode(() -> cobrarA(producto(NORMAL, REBAJA), NORMAL + 0.004)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sin precio no se puede cobrar")
    void sinPrecioNoSeCobra() {
        assertThatThrownBy(() -> cobrarA(producto(NORMAL, REBAJA), null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falta el precio");
    }
}
