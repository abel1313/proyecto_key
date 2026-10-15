package com.ventas.key.mis.productos.entity;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VariantesPrecioTest {

    private static Variantes articuloDe(double venta, double rebaja) {
        Producto p = new Producto();
        p.setPrecioVenta(venta);
        p.setPrecioRebaja(rebaja);
        Variantes v = new Variantes();
        v.setProducto(p);
        return v;
    }

    @Test
    @DisplayName("sin precio propio usa los del producto")
    void heredaDelProducto() {
        Variantes v = articuloDe(400, 350);
        assertThat(v.tienePrecioPropio()).isFalse();
        assertThat(v.precioNormal()).isEqualTo(400);
        assertThat(v.precioDescuento()).isEqualTo(350);
    }

    @Test
    @DisplayName("con precio propio usa los suyos, incluido el descuento en 0")
    void usaElPropio() {
        Variantes v = articuloDe(400, 350);
        v.setPrecioVenta(380.0);
        v.setPrecioRebaja(0.0);
        assertThat(v.tienePrecioPropio()).isTrue();
        assertThat(v.precioNormal()).isEqualTo(380);
        assertThat(v.precioDescuento()).isEqualTo(0);
    }

    @Test
    @DisplayName("R8: con usar descuento activo se cobra el descuento; sin activar, el normal")
    void precioACobrar() {
        Variantes v = articuloDe(400, 350);
        assertThat(v.precioACobrar()).isEqualTo(400);
        v.setUsarDescuento(true);
        assertThat(v.cobraConDescuento()).isTrue();
        assertThat(v.precioACobrar()).isEqualTo(350);
    }

    @Test
    @DisplayName("R8: activo pero sin descuento valido se cobra el normal")
    void usarDescuentoSinDescuentoValido() {
        Variantes igual = articuloDe(400, 400);
        igual.setUsarDescuento(true);
        assertThat(igual.cobraConDescuento()).isFalse();
        assertThat(igual.precioACobrar()).isEqualTo(400);

        Variantes cero = articuloDe(400, 0);
        cero.setUsarDescuento(true);
        assertThat(cero.precioACobrar()).isEqualTo(400);
    }
}
