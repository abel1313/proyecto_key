package com.ventas.key.hexagonal.articulo.dominio.modelo;

import com.ventas.key.hexagonal.articulo.dominio.excepcion.ArticuloNoVendibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** R4: al vender se valida modelo (habilitado, stock) y despues articulo (habilitado, stock). */
class ArticuloAVenderTest {

    @Test
    @DisplayName("modelo y articulo habilitados con stock: se vende")
    void seVende() {
        assertThatCode(() -> ArticuloAVender.deArticulo("Blusa", true, 5, true, 3).exigirQueSePuedaVender(3))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("producto deshabilitado: no se vende aunque el articulo este habilitado y con stock")
    void productoDeshabilitado() {
        assertThatThrownBy(() -> ArticuloAVender.deArticulo("Blusa", false, 5, true, 3).exigirQueSePuedaVender(1))
                .isInstanceOf(ArticuloNoVendibleException.class)
                .hasMessageContaining("el producto está deshabilitado");
    }

    @Test
    @DisplayName("el producto se revisa antes que el articulo")
    void productoPrimero() {
        assertThatThrownBy(() -> ArticuloAVender.deArticulo("Blusa", false, 0, false, 0).exigirQueSePuedaVender(1))
                .hasMessageContaining("el producto está deshabilitado");
    }

    @Test
    @DisplayName("producto sin stock suficiente")
    void productoSinStock() {
        assertThatThrownBy(() -> ArticuloAVender.deArticulo("Blusa", true, 1, true, 3).exigirQueSePuedaVender(2))
                .hasMessageContaining("Disponible: 1, solicitado: 2");
    }

    @Test
    @DisplayName("articulo deshabilitado: no se vende")
    void articuloDeshabilitado() {
        assertThatThrownBy(() -> ArticuloAVender.deArticulo("Blusa", true, 5, false, 3).exigirQueSePuedaVender(1))
                .hasMessageContaining("el artículo está deshabilitado");
    }

    @Test
    @DisplayName("articulo sin stock suficiente")
    void articuloSinStock() {
        assertThatThrownBy(() -> ArticuloAVender.deArticulo("Blusa", true, 5, true, 1).exigirQueSePuedaVender(2))
                .hasMessageContaining("Disponible: 1, solicitado: 2");
    }

    @Test
    @DisplayName("linea sin articulo: solo se revisa el producto")
    void soloModelo() {
        assertThatCode(() -> ArticuloAVender.soloModelo("Blusa", true, 2).exigirQueSePuedaVender(2))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ArticuloAVender.soloModelo("Blusa", false, 2).exigirQueSePuedaVender(1))
                .hasMessageContaining("el producto está deshabilitado");
    }
}
