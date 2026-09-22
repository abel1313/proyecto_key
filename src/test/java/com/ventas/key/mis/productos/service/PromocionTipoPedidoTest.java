package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Promocion;
import com.ventas.key.mis.productos.entity.PromocionDetalle;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IPromocionRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Una promocion se puede pagar de contado o apartar, pero no fiar.
 *
 * <p>Hasta el 2026-09-22 solo se aceptaba NORMAL, asi que la pantalla no dejaba otra salida que
 * el pago en efectivo: cualquier otro tipo reventaba en la primera linea de la validacion.
 */
class PromocionTipoPedidoTest {

    private IPromocionRepository promocionRepo;
    private PromocionServiceImpl service;

    private static final int PROMOCION_ID = 7;
    private static final int VARIANTE_ID = 42;
    private static final double PRECIO_PROMO = 250.0;

    @BeforeEach
    void setUp() {
        promocionRepo = mock(IPromocionRepository.class);
        service = new PromocionServiceImpl(
                promocionRepo,
                mock(IVarianteRepository.class),
                mock(IVarianteImagenRepository.class),
                mock(IClienteRepository.class),
                mock(EmailService.class));

        when(promocionRepo.findByIdConDetalle(PROMOCION_ID)).thenReturn(Optional.of(promocionVigente()));
    }

    private static Promocion promocionVigente() {
        Variantes variante = new Variantes();
        variante.setId(VARIANTE_ID);

        PromocionDetalle detalle = new PromocionDetalle();
        detalle.setVariante(variante);
        detalle.setCantidad(1);
        detalle.setPrecioEnPromocion(PRECIO_PROMO);

        Promocion promo = new Promocion();
        promo.setId(PROMOCION_ID);
        promo.setDescripcion("2x1 en jeans");
        promo.setActivo(true);
        promo.setFechaVencimiento(LocalDateTime.now().plusDays(7));
        promo.setDetalles(List.of(detalle));
        return promo;
    }

    private void validarCon(String tipoPedido) {
        service.validarLineasPromocion(
                PROMOCION_ID,
                List.of(new PromocionServiceImpl.LineaPromocionCheck(VARIANTE_ID, 1, PRECIO_PROMO)),
                tipoPedido);
    }

    @Test
    @DisplayName("de contado se puede, como siempre")
    void contadoSePuede() {
        assertThatCode(() -> validarCon("NORMAL")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("apartar una promocion ahora se puede: la mercancia no sale hasta pagarse")
    void apartarSePuede() {
        assertThatCode(() -> validarCon("APARTADO")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sin tipo de pedido se trata como contado")
    void sinTipoSePuede() {
        assertThatCode(() -> validarCon(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("fiar una promocion sigue prohibido: el producto sale sin estar pagado")
    void fiarSigueProhibido() {
        assertThatThrownBy(() -> validarCon("FIADO"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("credito");
    }

    @Test
    @DisplayName("una promocion vencida no se puede apartar")
    void vencidaNoSeAparta() {
        Promocion vencida = promocionVigente();
        vencida.setFechaVencimiento(LocalDateTime.now().minusDays(1));
        when(promocionRepo.findByIdConDetalle(PROMOCION_ID)).thenReturn(Optional.of(vencida));

        assertThatThrownBy(() -> validarCon("APARTADO"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya no esta disponible");
    }

    @Test
    @DisplayName("apartar no afloja la validacion del precio")
    void apartarNoAflojaElPrecio() {
        assertThatThrownBy(() -> service.validarLineasPromocion(
                PROMOCION_ID,
                List.of(new PromocionServiceImpl.LineaPromocionCheck(VARIANTE_ID, 1, 1.0)),
                "APARTADO"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no coincide");
    }
}
