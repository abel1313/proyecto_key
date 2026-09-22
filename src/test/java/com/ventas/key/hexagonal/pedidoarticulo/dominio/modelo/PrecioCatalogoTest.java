package com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R2: un articulo se cobra al precio normal o al de rebaja, y a ningun otro.
 *
 * <p>Es lo que impide que el front invente un monto al editar un pedido. Sin esta regla, el
 * blindaje que se le puso a la creacion de pedidos el 2026-09-22 no serviria de nada: bastaria
 * crear el pedido bien y despues editarlo.
 */
class PrecioCatalogoTest {

    private static final String NOMBRE = "Great Jeans";

    private static PrecioCatalogo con(Double normal, Double rebaja) {
        return new PrecioCatalogo(NOMBRE, normal, rebaja);
    }

    @Test
    @DisplayName("el precio normal se admite")
    void elNormalSeAdmite() {
        assertThat(con(400.0, 350.0).admite(400.0)).isTrue();
    }

    @Test
    @DisplayName("el precio de rebaja se admite -- es el tercer precio que antes no se cobraba")
    void laRebajaSeAdmite() {
        assertThat(con(400.0, 350.0).admite(350.0)).isTrue();
    }

    @Test
    @DisplayName("cualquier otro monto se rechaza")
    void otroMontoNo() {
        PrecioCatalogo precios = con(400.0, 350.0);
        assertThat(precios.admite(1.0)).isFalse();
        assertThat(precios.admite(399.0)).isFalse();
        assertThat(precios.admite(0.0)).isFalse();
    }

    @Test
    @DisplayName("una rebaja en 0 significa 'sin rebaja', no 'gratis'")
    void rebajaEnCeroNoEsGratis() {
        assertThat(con(400.0, 0.0).admite(0.0)).isFalse();
        assertThat(con(400.0, 0.0).tieneRebaja()).isFalse();
    }

    @Test
    @DisplayName("sin rebaja cargada solo vale el normal")
    void sinRebajaSoloNormal() {
        PrecioCatalogo precios = con(400.0, null);
        assertThat(precios.admite(400.0)).isTrue();
        assertThat(precios.admite(350.0)).isFalse();
    }

    @Test
    @DisplayName("tolera la diferencia de centavos que deja el redondeo del front")
    void toleraCentavos() {
        assertThat(con(400.0, 350.0).admite(400.004)).isTrue();
        assertThat(con(400.0, 350.0).admite(400.5)).isFalse();
    }

    @Test
    @DisplayName("null no es un precio")
    void nullNoEsPrecio() {
        assertThat(con(400.0, 350.0).admite(null)).isFalse();
    }

    @Test
    @DisplayName("sin precio pedido se cobra el normal, nunca la rebaja")
    void porDefectoElNormal() {
        // La rebaja es una decision que alguien toma, no algo que se aplique por omision.
        assertThat(con(400.0, 350.0).porDefecto()).isEqualTo(400.0);
    }

    @Test
    @DisplayName("la explicacion nombra los precios que si valen")
    void laExplicacionAyuda() {
        assertThat(con(400.0, 350.0).explicacion()).contains("400").contains("350");
        assertThat(con(400.0, null).explicacion()).contains("400").contains("no tiene rebaja");
    }
}
