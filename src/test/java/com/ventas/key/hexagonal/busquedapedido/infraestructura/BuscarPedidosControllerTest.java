package com.ventas.key.hexagonal.busquedapedido.infraestructura;

import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.EstadoBuscado;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FiltroPedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FormaDeCobro;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.PaginaDePedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.CuandoSeEntrega;
import com.ventas.key.hexagonal.busquedapedido.dominio.puerto.entrada.BuscarPedidosCasoUso;
import com.ventas.key.hexagonal.busquedapedido.infraestructura.entrada.rest.BuscarPedidosController;
import com.ventas.key.hexagonal.busquedapedido.infraestructura.salida.persistencia.TarjetasDePedidoLector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Como se traducen los parametros de la URL al filtro, y los 400 con mensaje. */
class BuscarPedidosControllerTest {

    private BuscarPedidosCasoUso casoUso;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        casoUso = mock(BuscarPedidosCasoUso.class);
        TarjetasDePedidoLector tarjetas = mock(TarjetasDePedidoLector.class);
        when(tarjetas.tarjetas(anyList())).thenReturn(List.of());
        when(casoUso.buscar(any())).thenReturn(new PaginaDePedidos(List.of(), 21, 1, 10));
        mvc = MockMvcBuilders.standaloneSetup(new BuscarPedidosController(casoUso, tarjetas)).build();
    }

    @Test
    void traduce_los_parametros_y_regresa_los_totales() throws Exception {
        mvc.perform(get("/v1/pedidos/buscar")
                        .param("buscar", "maria")
                        .param("formaCobro", "apartado,IR_PAGANDO")
                        .param("estado", "POR_COBRAR").param("estado", "pagado")
                        .param("registroDesde", "2026-10-01")
                        .param("entrega", "atrasados")
                        .param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRegistros").value(21))
                .andExpect(jsonPath("$.data.totalPaginas").value(3))
                .andExpect(jsonPath("$.data.pagina").value(1));

        ArgumentCaptor<FiltroPedidos> f = ArgumentCaptor.forClass(FiltroPedidos.class);
        verify(casoUso).buscar(f.capture());
        assertThat(f.getValue().texto().valor()).isEqualTo("maria");
        assertThat(f.getValue().formas()).containsExactlyInAnyOrder(FormaDeCobro.APARTADO, FormaDeCobro.IR_PAGANDO);
        assertThat(f.getValue().estados()).containsExactlyInAnyOrder(EstadoBuscado.POR_COBRAR, EstadoBuscado.PAGADO);
        assertThat(f.getValue().registro().desde()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(f.getValue().entrega()).isEqualTo(CuandoSeEntrega.ATRASADOS);
        assertThat(f.getValue().pagina()).isEqualTo(1);
    }

    @Test
    void un_valor_que_no_existe_contesta_400_con_los_validos() throws Exception {
        mvc.perform(get("/v1/pedidos/buscar").param("estado", "ENVIADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("PENDIENTE, POR_COBRAR")));
        verify(casoUso, never()).buscar(any());
    }

    @Test
    void texto_corto_y_fechas_al_reves_contestan_400() throws Exception {
        mvc.perform(get("/v1/pedidos/buscar").param("buscar", "ma"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("3 letras")));
        mvc.perform(get("/v1/pedidos/buscar").param("registroDesde", "2026-10-05").param("registroHasta", "2026-10-01"))
                .andExpect(status().isBadRequest());
    }
}
