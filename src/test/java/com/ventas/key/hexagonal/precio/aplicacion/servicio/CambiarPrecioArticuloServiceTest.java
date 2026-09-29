package com.ventas.key.hexagonal.precio.aplicacion.servicio;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;
import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeArticulo;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.AvisarCambioCatalogoPort;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.PreciosArticuloPort;
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

class CambiarPrecioArticuloServiceTest {

    private PreciosArticuloPort precios;
    private AvisarCambioCatalogoPort catalogo;
    private CambiarPrecioArticuloService service;

    @BeforeEach
    void setUp() {
        precios = mock(PreciosArticuloPort.class);
        catalogo = mock(AvisarCambioCatalogoPort.class);
        service = new CambiarPrecioArticuloService(precios, catalogo);
    }

    @Test
    @DisplayName("guarda un precio propio para ese articulo y avisa al catalogo")
    void guardaPropio() {
        when(precios.buscar(7)).thenReturn(Optional.of(new PreciosDeArticulo(7, "Blusa M", 200, 400, 0, false)));

        PreciosDeArticulo r = service.cambiar(7, 380.0, 350.0);

        ArgumentCaptor<PreciosDeArticulo> guardado = ArgumentCaptor.forClass(PreciosDeArticulo.class);
        verify(precios).guardar(guardado.capture());
        assertThat(guardado.getValue().propio()).isTrue();
        assertThat(guardado.getValue().precioVenta()).isEqualTo(380);
        assertThat(r.precioRebaja()).isEqualTo(350);
        verify(catalogo).catalogoCambio();
    }

    @Test
    @DisplayName("un precio invalido no guarda nada")
    void invalidoNoGuarda() {
        when(precios.buscar(7)).thenReturn(Optional.of(new PreciosDeArticulo(7, "Blusa M", 200, 400, 0, false)));

        assertThatThrownBy(() -> service.cambiar(7, 380.0, 400.0)).isInstanceOf(PrecioInvalidoException.class);
        verify(precios, never()).guardar(any());
        verify(catalogo, never()).catalogoCambio();
    }

    @Test
    @DisplayName("volver al del producto borra el propio y responde con el precio del producto")
    void vuelveAlDelProducto() {
        when(precios.buscar(7)).thenReturn(
                Optional.of(new PreciosDeArticulo(7, "Blusa M", 200, 380, 350, true)),
                Optional.of(new PreciosDeArticulo(7, "Blusa M", 200, 400, 0, false)));

        PreciosDeArticulo r = service.usarElDelProducto(7);

        ArgumentCaptor<PreciosDeArticulo> guardado = ArgumentCaptor.forClass(PreciosDeArticulo.class);
        verify(precios).guardar(guardado.capture());
        assertThat(guardado.getValue().propio()).isFalse();
        assertThat(r.precioVenta()).isEqualTo(400);
        verify(catalogo).catalogoCambio();
    }

    @Test
    @DisplayName("si ya usaba el del producto no hace nada")
    void yaHeredaba() {
        when(precios.buscar(7)).thenReturn(Optional.of(new PreciosDeArticulo(7, "Blusa M", 200, 400, 0, false)));

        service.usarElDelProducto(7);

        verify(precios, never()).guardar(any());
        verify(catalogo, never()).catalogoCambio();
    }

    @Test
    @DisplayName("articulo inexistente")
    void noExiste() {
        when(precios.buscar(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cambiar(99, 100.0, 0.0)).isInstanceOf(PrecioInvalidoException.class);
    }
}
