package com.ventas.key.hexagonal.articulo.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R1 y R2 del alta de articulos (reportado 2026-09-22).
 *
 * <p>El caso que las origina: <i>"si en tallas solo agrego 2, aparece que voy a guardar 3 aunque
 * este vacio lo que llene"</i> -- el formulario base seguia viajando despues de vaciarlo, y nacia
 * un tercer articulo sin talla, sin color y sin nada.
 */
class ArticuloDeAltaTest {

    /** El formulario base despues de vaciarlo: nuevo, sin nada, sin stock, sin fotos. */
    private static ArticuloDeAlta vacio() {
        return new ArticuloDeAlta(null, null, null, null, null, null, null, 0, false, null);
    }

    private static ArticuloDeAlta conTalla(String talla, int stock) {
        return new ArticuloDeAlta(null, talla, null, null, null, null, null, stock, false, null);
    }

    // ───────────────────────── R1: que cuenta como articulo ─────────────────────────

    @Test
    @DisplayName("el formulario base vaciado no se guarda -- es el bug del contador")
    void elVacioNoSeGuarda() {
        assertThat(vacio().describeAlgo()).isFalse();
    }

    @Test
    @DisplayName("campos en blanco cuentan como vacio, no como dato")
    void losEspaciosNoSonDato() {
        ArticuloDeAlta soloEspacios =
                new ArticuloDeAlta(null, "   ", "", "  ", null, "", " ", 0, false, null);

        assertThat(soloEspacios.describeAlgo()).isFalse();
    }

    @Test
    @DisplayName("con talla se guarda")
    void conTallaSeGuarda() {
        assertThat(conTalla("M", 0).describeAlgo()).isTrue();
    }

    @Test
    @DisplayName("basta UN campo propio, no hacen falta todos")
    void bastaUnCampo() {
        // Un articulo que solo se distingue por el color es tan valido como uno con talla y marca.
        ArticuloDeAlta soloColor =
                new ArticuloDeAlta(null, null, "rojo", null, null, null, null, 0, false, null);

        assertThat(soloColor.describeAlgo()).isTrue();
        assertThat(soloColor.tieneAlgunDatoPropio()).isTrue();
    }

    @Test
    @DisplayName("sin datos pero CON stock se guarda: es el modelo de un solo articulo")
    void conStockSeGuardaAunqueNoTengaTalla() {
        // "Si solo quiero agregar 1 articulo, lleno los datos que estan" -- un modelo sin tallas.
        assertThat(conTalla(null, 5).describeAlgo()).isTrue();
    }

    @Test
    @DisplayName("sin datos pero CON imagenes se guarda")
    void conImagenesSeGuarda() {
        ArticuloDeAlta soloFotos =
                new ArticuloDeAlta(null, null, null, null, null, null, null, 0, true, null);

        assertThat(soloFotos.describeAlgo()).isTrue();
    }

    @Test
    @DisplayName("uno que YA EXISTE nunca se descarta, aunque le vacien todo")
    void elQueYaExisteNoSeDescarta() {
        // Vaciarle los campos a un articulo guardado es una edicion valida, no un descarte.
        ArticuloDeAlta existenteVaciado =
                new ArticuloDeAlta(77, null, null, null, null, null, null, 0, false, null);

        assertThat(existenteVaciado.describeAlgo()).isTrue();
        assertThat(existenteVaciado.yaExiste()).isTrue();
    }

    @Test
    @DisplayName("stock negativo no cuenta como stock")
    void stockNegativoNoCuenta() {
        assertThat(conTalla(null, -3).describeAlgo()).isFalse();
    }

    // ───────────────────────── R2: la categoria se hereda ─────────────────────────

    @Test
    @DisplayName("sin categoria propia toma la del modelo")
    void heredaLaCategoriaDelModelo() {
        assertThat(conTalla("M", 1).categoriaEfectiva(9)).isEqualTo(9);
    }

    @Test
    @DisplayName("con categoria propia conserva la suya -- el modelo no la pisa")
    void laPropiaGana() {
        ArticuloDeAlta conCategoria =
                new ArticuloDeAlta(null, "M", null, null, null, null, null, 1, false, 4);

        assertThat(conCategoria.categoriaEfectiva(9)).isEqualTo(4);
    }

    @Test
    @DisplayName("si el modelo tampoco tiene, queda sin categoria")
    void sinCategoriaEnNingunLado() {
        assertThat(conTalla("M", 1).categoriaEfectiva(null)).isNull();
    }
}
