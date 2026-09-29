package com.ventas.key.hexagonal.preferenciafiltro.infraestructura.entrada.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion.PreferenciaFiltroException;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion.SoloPersonalException;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.FiltrosGuardados;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.Pantalla;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.entrada.FiltrosGuardadosCasoUso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PreferenciaFiltroControllerTest {

    private final FiltrosGuardadosCasoUso casoUso = mock(FiltrosGuardadosCasoUso.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new PreferenciaFiltroController(casoUso, new ObjectMapper().registerModule(new JavaTimeModule()))).build();

    @Test
    @DisplayName("GET devuelve los filtros como objeto dentro de 'data'")
    void obtener() throws Exception {
        when(casoUso.obtener("tienda-buscar")).thenReturn(Optional.of(new FiltrosGuardados(
                4, Pantalla.TIENDA_BUSCAR, "{\"filtroTalla\":\"M\"}", LocalDateTime.of(2026, 9, 30, 10, 0))));

        mvc.perform(get("/v1/preferencias-filtro/tienda-buscar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pantalla").value("tienda-buscar"))
                .andExpect(jsonPath("$.data.filtros.filtroTalla").value("M"));
    }

    @Test
    @DisplayName("GET sin filtros guardados responde 204")
    void obtenerSinNada() throws Exception {
        when(casoUso.obtener("tienda-buscar")).thenReturn(Optional.empty());

        mvc.perform(get("/v1/preferencias-filtro/tienda-buscar")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT manda al caso de uso solo el objeto 'filtros' como JSON")
    void guardar() throws Exception {
        when(casoUso.guardar(eq("productos-buscar"), anyString())).thenReturn(Optional.of(new FiltrosGuardados(
                4, Pantalla.PRODUCTOS_BUSCAR, "{\"mostrarConStock\":true}", LocalDateTime.now())));

        mvc.perform(put("/v1/preferencias-filtro/productos-buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filtros\":{\"mostrarConStock\":true}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.filtros.mostrarConStock").value(true));
        verify(casoUso).guardar("productos-buscar", "{\"mostrarConStock\":true}");
    }

    @Test
    @DisplayName("DELETE responde 204")
    void borrar() throws Exception {
        mvc.perform(delete("/v1/preferencias-filtro/tienda-buscar")).andExpect(status().isNoContent());
        verify(casoUso).borrar("tienda-buscar");
    }

    @Test
    @DisplayName("cliente -> 403 y pantalla desconocida -> 400, con el mensaje en 'mensaje'")
    void errores() throws Exception {
        when(casoUso.obtener("tienda-buscar")).thenThrow(new SoloPersonalException());
        when(casoUso.obtener("pedidos")).thenThrow(PreferenciaFiltroException.pantallaDesconocida("pedidos"));

        mvc.perform(get("/v1/preferencias-filtro/tienda-buscar"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Guardar filtros es solo para el personal de la tienda"));
        mvc.perform(get("/v1/preferencias-filtro/pedidos")).andExpect(status().isBadRequest());
    }
}
