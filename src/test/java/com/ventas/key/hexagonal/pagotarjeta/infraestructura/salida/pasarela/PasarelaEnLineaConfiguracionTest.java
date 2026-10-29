package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela;

import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PasarelaDePagoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

/** Que la propiedad {@code pagos.tarjeta.pasarela-en-linea} arme las piezas correctas (y solo esas). */
class PasarelaEnLineaConfiguracionTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withBean(Clock.class, Clock::systemUTC)
            .withUserConfiguration(PasarelaSimuladaAdapter.class, MercadoPagoCheckoutSdk.class,
                    PasarelaCheckoutProAdapter.class, PasarelaPorCanalAdapter.class)
            .withPropertyValues("spring.profiles.active=qa", "pagos.tarjeta.pasarela=simulada",
                    "mercadopago.access-token=TEST-token-de-prueba",
                    "pagos.tarjeta.checkout.front-url=https://qa.shop.test",
                    "pagos.tarjeta.checkout.notificacion-url=https://qa.back.test/webhook");

    @Test
    @DisplayName("en línea 'simulada': solo la pasarela simulada, nada de Mercado Pago")
    void simulada() {
        contexto.withPropertyValues("pagos.tarjeta.pasarela-en-linea=simulada").run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c.getBeansOfType(PasarelaDePagoPort.class)).hasSize(1);
            assertThat(c).hasSingleBean(PasarelaSimuladaAdapter.class);
            assertThat(c).doesNotHaveBean(PasarelaCheckoutProAdapter.class);
            assertThat(c).doesNotHaveBean(MercadoPagoCheckoutApi.class);
        });
    }

    @Test
    @DisplayName("en línea 'checkout': la pasarela del dominio es la que reparte por canal (terminal sigue simulada)")
    void checkout() {
        contexto.withPropertyValues("pagos.tarjeta.pasarela-en-linea=checkout").run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c.getBean(PasarelaDePagoPort.class)).isInstanceOf(PasarelaPorCanalAdapter.class);
            assertThat(c).hasSingleBean(PasarelaCheckoutProAdapter.class);
            assertThat(c).hasSingleBean(MercadoPagoCheckoutApi.class);
        });
    }
}
