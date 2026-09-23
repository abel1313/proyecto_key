package com.ventas.key.hexagonal.precio.dominio.modelo;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreciosDeProductoTest {

    private static final PreciosDeProducto BLUSA = new PreciosDeProducto(10, "Blusa", 200, 400, 0);

    @Test
    @DisplayName("bajar el precio con descuento: se cobra el descuento")
    void bajaConDescuento() {
        PreciosDeProducto nuevo = BLUSA.conPrecios(400.0, 350.0);

        assertThat(nuevo.precioRebaja()).isEqualTo(350);
        assertThat(nuevo.precioACobrar()).isEqualTo(350);
    }

    @Test
    @DisplayName("subir el precio normal")
    void subeElNormal() {
        PreciosDeProducto nuevo = BLUSA.conPrecios(450.0, 0.0);

        assertThat(nuevo.precioVenta()).isEqualTo(450);
        assertThat(nuevo.precioACobrar()).isEqualTo(450);
    }

    @Test
    @DisplayName("descuento null queda como sin descuento")
    void descuentoNullEsCero() {
        assertThat(BLUSA.conPrecios(400.0, null).precioRebaja()).isZero();
    }

    @Test
    @DisplayName("el precio normal es obligatorio y mayor a 0")
    void normalObligatorio() {
        assertThatThrownBy(() -> BLUSA.conPrecios(null, 100.0)).isInstanceOf(PrecioInvalidoException.class);
        assertThatThrownBy(() -> BLUSA.conPrecios(0.0, 0.0)).hasMessageContaining("mayor a 0");
    }

    @Test
    @DisplayName("el descuento no puede ser negativo")
    void descuentoNegativo() {
        assertThatThrownBy(() -> BLUSA.conPrecios(400.0, -1.0)).hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("el descuento no puede ser mayor al normal: para cobrar mas se sube el normal")
    void descuentoMayorQueNormal() {
        assertThatThrownBy(() -> BLUSA.conPrecios(400.0, 500.0))
                .hasMessageContaining("no puede ser mayor al normal")
                .hasMessageContaining("sube el precio normal");
    }

    @Test
    @DisplayName("vender por debajo del costo se permite pero se marca")
    void bajoCosto() {
        assertThat(BLUSA.conPrecios(400.0, 150.0).vendeBajoCosto()).isTrue();
        assertThat(BLUSA.conPrecios(400.0, 250.0).vendeBajoCosto()).isFalse();
    }

    @Test
    @DisplayName("sin costo cargado nunca avisa bajo costo")
    void sinCosto() {
        PreciosDeProducto sinCosto = new PreciosDeProducto(1, "X", 0, 100, 0);
        assertThat(sinCosto.conPrecios(10.0, 0.0).vendeBajoCosto()).isFalse();
    }
}
