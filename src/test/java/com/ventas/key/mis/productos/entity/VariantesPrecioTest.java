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
}
