package com.ventas.key.hexagonal.precio.aplicacion.servicio;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;
import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeProducto;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.AvisarCambioCatalogoPort;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.PreciosProductoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CambiarPrecioServiceTest {

    private PreciosProductoPort precios;
    private AvisarCambioCatalogoPort catalogo;
    private CambiarPrecioService service;

    @BeforeEach
    void setUp() {
        precios = mock(PreciosProductoPort.class);
        catalogo = mock(AvisarCambioCatalogoPort.class);
        service = new CambiarPrecioService(precios, catalogo);
    }

    @Test
    @DisplayName("guarda los precios nuevos y avisa al catalogo")
    void guardaYAvisa() {
        when(precios.buscar(10)).thenReturn(Optional.of(new PreciosDeProducto(10, "Blusa", 200, 400, 0)));

        service.cambiar(10, 400.0, 350.0);

        ArgumentCaptor<PreciosDeProducto> guardado = ArgumentCaptor.forClass(PreciosDeProducto.class);
        verify(precios).guardar(guardado.capture());
        assertThat(guardado.getValue().precioRebaja()).isEqualTo(350);
        verify(catalogo).catalogoCambio();
    }

    @Test
    @DisplayName("un precio invalido no guarda nada ni toca la cache")
    void invalidoNoGuarda() {
        when(precios.buscar(10)).thenReturn(Optional.of(new PreciosDeProducto(10, "Blusa", 200, 400, 0)));

        assertThatThrownBy(() -> service.cambiar(10, 400.0, 900.0)).isInstanceOf(PrecioInvalidoException.class);
        verify(precios, never()).guardar(any());
        verify(catalogo, never()).catalogoCambio();
    }

    @Test
    @DisplayName("producto inexistente")
    void noExiste() {
        when(precios.buscar(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiar(99, 100.0, 0.0)).hasMessageContaining("No existe el producto 99");
    }
}
