package com.ventas.key.hexagonal.preferenciafiltro.aplicacion.servicio;

import com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion.PreferenciaFiltroException;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion.SoloPersonalException;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.FiltrosGuardados;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.Pantalla;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.QuienGuarda;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.salida.FiltrosGuardadosPort;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.salida.QuienGuardaPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FiltrosGuardadosServiceTest {

    private final FiltrosGuardadosPort filtros = mock(FiltrosGuardadosPort.class);
    private final QuienGuardaPort quienGuarda = mock(QuienGuardaPort.class);
    private final FiltrosGuardadosService service = new FiltrosGuardadosService(filtros, quienGuarda);

    private static final String JSON = "{\"mostrarConStock\":true}";

    @Test
    @DisplayName("R1: un cliente no lee, no guarda ni borra; ni siquiera se consulta la base")
    void clienteNo() {
        when(quienGuarda.actual()).thenReturn(new QuienGuarda(9, false));

        assertThatThrownBy(() -> service.obtener("tienda-buscar")).isInstanceOf(SoloPersonalException.class);
        assertThatThrownBy(() -> service.guardar("tienda-buscar", JSON)).isInstanceOf(SoloPersonalException.class);
        assertThatThrownBy(() -> service.borrar("tienda-buscar")).isInstanceOf(SoloPersonalException.class);
        verifyNoInteractions(filtros);
    }

    @Test
    @DisplayName("R3: se guarda con el usuario del token y la hora del momento")
    void guardaDelUsuarioActual() {
        when(quienGuarda.actual()).thenReturn(new QuienGuarda(4, true));
        when(filtros.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<FiltrosGuardados> r = service.guardar("productos-buscar", JSON);

        ArgumentCaptor<FiltrosGuardados> guardado = ArgumentCaptor.forClass(FiltrosGuardados.class);
        verify(filtros).guardar(guardado.capture());
        assertThat(guardado.getValue().usuarioId()).isEqualTo(4);
        assertThat(guardado.getValue().pantalla()).isEqualTo(Pantalla.PRODUCTOS_BUSCAR);
        assertThat(guardado.getValue().filtrosJson()).isEqualTo(JSON);
        assertThat(guardado.getValue().actualizado()).isNotNull();
        assertThat(r).isPresent();
    }

    @Test
    @DisplayName("R7: guardar {} borra lo guardado y no crea fila")
    void vacioBorra() {
        when(quienGuarda.actual()).thenReturn(new QuienGuarda(4, true));

        Optional<FiltrosGuardados> r = service.guardar("tienda-buscar", "{}");

        assertThat(r).isEmpty();
        verify(filtros).borrar(4, Pantalla.TIENDA_BUSCAR);
        verify(filtros, never()).guardar(any());
    }

    @Test
    @DisplayName("obtener y borrar usan el usuario del token")
    void obtenerYBorrar() {
        when(quienGuarda.actual()).thenReturn(new QuienGuarda(4, true));
        FiltrosGuardados f = new FiltrosGuardados(4, Pantalla.TIENDA_BUSCAR, JSON, LocalDateTime.now());
        when(filtros.buscar(4, Pantalla.TIENDA_BUSCAR)).thenReturn(Optional.of(f));

        assertThat(service.obtener("tienda-buscar")).contains(f);
        service.borrar("tienda-buscar");
        verify(filtros).borrar(4, Pantalla.TIENDA_BUSCAR);
    }

    @Test
    @DisplayName("R2: una pantalla que no guarda filtros se rechaza")
    void pantallaDesconocida() {
        when(quienGuarda.actual()).thenReturn(new QuienGuarda(4, true));

        assertThatThrownBy(() -> service.guardar("pedidos", JSON)).isInstanceOf(PreferenciaFiltroException.class);
        verifyNoInteractions(filtros);
    }
}
