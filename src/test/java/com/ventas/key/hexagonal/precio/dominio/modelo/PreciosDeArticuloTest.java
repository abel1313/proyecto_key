package com.ventas.key.hexagonal.precio.dominio.modelo;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreciosDeArticuloTest {

    private final PreciosDeArticulo heredado = new PreciosDeArticulo(7, "Blusa talla M", 200, 400, 350, false);

    @Test
    @DisplayName("el precio propio puede ser mas alto o mas bajo que el del producto")
    void subeOBaja() {
        assertThat(heredado.conPrecios(450.0, 0.0).precioVenta()).isEqualTo(450);
        assertThat(heredado.conPrecios(300.0, 280.0).precioRebaja()).isEqualTo(280);
        assertThat(heredado.conPrecios(300.0, null).propio()).isTrue();
    }

    @Test
    @DisplayName("mismas reglas que el producto: normal > 0 y descuento entre 0 y el normal")
    void mismasReglas() {
        assertThatThrownBy(() -> heredado.conPrecios(0.0, 0.0)).isInstanceOf(PrecioInvalidoException.class);
        assertThatThrownBy(() -> heredado.conPrecios(300.0, -1.0)).isInstanceOf(PrecioInvalidoException.class);
        assertThatThrownBy(() -> heredado.conPrecios(300.0, 301.0)).isInstanceOf(PrecioInvalidoException.class);
    }

    @Test
    @DisplayName("avisa si el descuento queda por debajo del costo")
    void bajoCosto() {
        assertThat(heredado.conPrecios(400.0, 150.0).vendeBajoCosto()).isTrue();
        assertThat(heredado.conPrecios(400.0, 0.0).vendeBajoCosto()).isFalse();
    }

    @Test
    @DisplayName("heredando deja de tener precio propio")
    void heredando() {
        assertThat(heredado.conPrecios(450.0, 0.0).heredando().propio()).isFalse();
    }
}
