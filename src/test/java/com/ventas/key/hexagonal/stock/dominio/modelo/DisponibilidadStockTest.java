package com.ventas.key.hexagonal.stock.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El calculo de "cuanto queda libre" vive en el modelo, asi que se prueba sin mocks ni Spring.
 *
 * <p>Los numeros de varios casos salen de produccion al 2026-09-22.
 */
class DisponibilidadStockTest {

    private static DisponibilidadStock con(int total, int enVariantes, int activas, int deBaja) {
        return new DisponibilidadStock(279, "Great Jeans", total, enVariantes, activas, deBaja);
    }

    @Test
    @DisplayName("el disponible es lo que el producto tiene menos lo repartido")
    void disponibleEsLaResta() {
        assertThat(con(10, 4, 2, 0).disponible()).isEqualTo(6);
    }

    @Test
    @DisplayName("lo que retienen las variantes dadas de baja no descuenta")
    void lasDeBajaNoDescuentan() {
        // 10 en total, 2 repartidos en activas, y 8 "colgados" en una dada de baja.
        // Ese era el bug: se contaban los 8 y el disponible daba 0.
        assertThat(con(10, 2, 1, 8).disponible()).isEqualTo(8);
    }

    @Test
    @DisplayName("sin variantes, todo el stock esta disponible")
    void sinVariantesTodoDisponible() {
        assertThat(con(10, 0, 0, 0).disponible()).isEqualTo(10);
    }

    @Test
    @DisplayName("todo repartido deja cero, y eso no es estar descuadrado")
    void todoRepartidoDejaCero() {
        DisponibilidadStock d = con(10, 10, 5, 0);

        assertThat(d.disponible()).isZero();
        assertThat(d.estaDescuadrado()).isFalse();
    }

    @Test
    @DisplayName("si las variantes piden mas de lo que hay, el producto esta descuadrado")
    void masEnVariantesQueEnProductoEsDescuadre() {
        // Producto 269 en produccion: 12 en el producto, 18 repartidos en variantes.
        DisponibilidadStock d = new DisponibilidadStock(269, "Jeans Short Especal", 12, 18, 6, 0);

        assertThat(d.disponible()).isEqualTo(-6);
        assertThat(d.estaDescuadrado()).isTrue();
    }

    @Test
    @DisplayName("el negativo NO se recorta a cero: es un problema que hay que ver")
    void elNegativoNoSeEsconde() {
        // Recortar a cero dejaria la pantalla diciendo "0 disponibles", que se lee como
        // "esta lleno" cuando en realidad significa "estos datos estan mal".
        assertThat(con(1, 11, 4, 0).disponible()).isEqualTo(-10);
    }
}
