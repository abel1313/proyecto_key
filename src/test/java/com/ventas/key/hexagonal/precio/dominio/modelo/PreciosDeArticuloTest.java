package com.ventas.key.hexagonal.precio.dominio.modelo;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreciosDeArticuloTest {

    private final PreciosDeArticulo heredado = new PreciosDeArticulo(7, "Blusa talla M", 200, 400, 350, false, false);

    @Test
    @DisplayName("el precio propio puede ser mas alto o mas bajo que el del producto")
    void subeOBaja() {
        assertThat(heredado.conPrecios(450.0, 0.0, false).precioVenta()).isEqualTo(450);
        assertThat(heredado.conPrecios(300.0, 280.0, false).precioRebaja()).isEqualTo(280);
        assertThat(heredado.conPrecios(300.0, null, false).propio()).isTrue();
    }

    @Test
    @DisplayName("mismas reglas que el producto: normal > 0 y descuento entre 0 y el normal")
    void mismasReglas() {
        assertThatThrownBy(() -> heredado.conPrecios(0.0, 0.0, false)).isInstanceOf(PrecioInvalidoException.class);
        assertThatThrownBy(() -> heredado.conPrecios(300.0, -1.0, false)).isInstanceOf(PrecioInvalidoException.class);
        assertThatThrownBy(() -> heredado.conPrecios(300.0, 301.0, false)).isInstanceOf(PrecioInvalidoException.class);
    }

    @Test
    @DisplayName("avisa si el descuento queda por debajo del costo")
    void bajoCosto() {
        assertThat(heredado.conPrecios(400.0, 150.0, false).vendeBajoCosto()).isTrue();
        assertThat(heredado.conPrecios(400.0, 0.0, false).vendeBajoCosto()).isFalse();
    }

    @Test
    @DisplayName("heredando deja de tener precio propio")
    void heredando() {
        assertThat(heredado.conPrecios(450.0, 0.0, false).heredando().propio()).isFalse();
    }

    @Test
    @DisplayName("R8: con usar descuento se vende al descuento")
    void usarDescuento() {
        PreciosDeArticulo p = heredado.conPrecios(400.0, 300.0, true);
        assertThat(p.usarDescuento()).isTrue();
        assertThat(p.precioACobrar()).isEqualTo(300);
        assertThat(heredado.conPrecios(400.0, 300.0, false).precioACobrar()).isEqualTo(400);
    }

    @Test
    @DisplayName("R8: para usarlo, el descuento tiene que existir y ser menor al normal")
    void usarDescuentoSinDescuento() {
        assertThatThrownBy(() -> heredado.conPrecios(400.0, 0.0, true)).hasMessageContaining("menor al normal");
        assertThatThrownBy(() -> heredado.conPrecios(400.0, 400.0, true)).hasMessageContaining("menor al normal");
    }

    @Test
    @DisplayName("R8: volver al del producto apaga el descuento activo")
    void heredandoApagaDescuento() {
        assertThat(heredado.conPrecios(400.0, 300.0, true).heredando().usarDescuento()).isFalse();
    }
}
