package com.ventas.key.hexagonal.precio.aplicacion.servicio;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;
import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeArticulo;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.PreciosArticuloPort;
import com.ventas.key.hexagonal.precio.infraestructura.dto.DescuentoArticuloResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarDescuentoArticuloServiceTest {

    private final PreciosArticuloPort precios = mock(PreciosArticuloPort.class);
    private final ConsultarDescuentoArticuloService service = new ConsultarDescuentoArticuloService(precios);

    @Test
    @DisplayName("R9: devuelve el descuento cobrable de un solo articulo")
    void conDescuento() {
        when(precios.buscar(7)).thenReturn(Optional.of(new PreciosDeArticulo(7, "Blusa M", 200, 400, 350, false, false)));
        DescuentoArticuloResponse r = DescuentoArticuloResponse.de(service.consultar(7));
        assertThat(r.tieneDescuento()).isTrue();
        assertThat(r.precioRebaja()).isEqualTo(350);
    }

    @Test
    @DisplayName("R9: un descuento igual al normal (el default al dar de alta) no cuenta como descuento")
    void descuentoIgualAlNormal() {
        when(precios.buscar(7)).thenReturn(Optional.of(new PreciosDeArticulo(7, "Blusa M", 200, 400, 400, false, false)));
        DescuentoArticuloResponse r = DescuentoArticuloResponse.de(service.consultar(7));
        assertThat(r.tieneDescuento()).isFalse();
        assertThat(r.precioRebaja()).isZero();
    }

    @Test
    @DisplayName("un articulo que no existe responde 400")
    void noExiste() {
        when(precios.buscar(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.consultar(99)).isInstanceOf(PrecioInvalidoException.class);
    }
}
