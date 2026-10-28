package com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ConfigMsiArticulo;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.AceptaMsiPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurarAceptaMsiServiceTest {

    private final AceptaMsiPort puerto = mock(AceptaMsiPort.class);
    private final ConfigurarAceptaMsiService servicio = new ConfigurarAceptaMsiService(puerto);

    @Test
    void elArticuloHeredaCuandoNoTieneCasillaPropia_yDecideElSuyoCuandoLaTiene() {
        assertThat(new ConfigMsiArticulo(null, true).efectivo()).isTrue();      // hereda: producto sí
        assertThat(new ConfigMsiArticulo(null, false).efectivo()).isFalse();    // hereda: producto no
        assertThat(new ConfigMsiArticulo(false, true).efectivo()).isFalse();    // propio manda sobre el producto
        assertThat(new ConfigMsiArticulo(true, false).efectivo()).isTrue();
        assertThat(new ConfigMsiArticulo(null, true).hereda()).isTrue();
        assertThat(new ConfigMsiArticulo(false, true).hereda()).isFalse();
    }

    @Test
    void leeYCambiaLaCasillaDeUnProducto() {
        when(puerto.productoAcepta(5)).thenReturn(Optional.of(true));
        when(puerto.fijarProducto(5, false)).thenReturn(true);
        assertThat(servicio.aceptaProducto(5)).isTrue();
        servicio.cambiarProducto(5, false);
        verify(puerto).fijarProducto(5, false);
    }

    @Test
    void unProductoQueNoExisteSeAvisa() {
        when(puerto.productoAcepta(99)).thenReturn(Optional.empty());
        when(puerto.fijarProducto(99, true)).thenReturn(false);
        assertThatThrownBy(() -> servicio.aceptaProducto(99)).isInstanceOf(PagoConTarjetaException.NoExiste.class)
                .hasMessageContaining("No existe el producto 99");
        assertThatThrownBy(() -> servicio.cambiarProducto(99, true)).isInstanceOf(PagoConTarjetaException.NoExiste.class);
    }

    @Test
    void unArticuloPuedeVolverAHeredar_conNull() {
        when(puerto.fijarArticulo(7, null)).thenReturn(true);
        servicio.cambiarArticulo(7, null);
        verify(puerto).fijarArticulo(7, null);
    }

    @Test
    void unArticuloQueNoExisteSeAvisa() {
        when(puerto.articulo(88)).thenReturn(Optional.empty());
        when(puerto.fijarArticulo(88, true)).thenReturn(false);
        assertThatThrownBy(() -> servicio.articulo(88)).isInstanceOf(PagoConTarjetaException.NoExiste.class)
                .hasMessageContaining("No existe el artículo 88");
        assertThatThrownBy(() -> servicio.cambiarArticulo(88, true)).isInstanceOf(PagoConTarjetaException.NoExiste.class);
    }

    @Test
    void sinIdValidoNoTocaLaBase() {
        assertThatThrownBy(() -> servicio.cambiarProducto(null, true)).isInstanceOf(PagoConTarjetaException.class);
        assertThatThrownBy(() -> servicio.cambiarArticulo(0, true)).isInstanceOf(PagoConTarjetaException.class);
        verify(puerto, never()).fijarProducto(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void consultaVariosArticulosAlaVez_conTope() {
        when(puerto.efectivos(List.of(1, 2))).thenReturn(Map.of(1, true, 2, false));
        assertThat(servicio.efectivos(List.of(1, 2))).containsEntry(1, true).containsEntry(2, false);
        assertThat(servicio.efectivos(List.of())).isEmpty();
        List<Integer> muchos = IntStream.rangeClosed(1, 501).boxed().toList();
        assertThatThrownBy(() -> servicio.efectivos(muchos)).hasMessageContaining("500");
    }
}
