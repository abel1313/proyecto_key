package com.ventas.key.hexagonal.revisionventas.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.revisionventas.dominio.excepcion.RevisionVentasException;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.Hallazgo;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.HallazgosDeUnTipo;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.ResultadoRevision;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.TipoHallazgo;
import com.ventas.key.hexagonal.revisionventas.dominio.puerto.entrada.RevisarVentasCasoUso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RevisionVentasControllerTest {

    private final RevisarVentasCasoUso revisar = mock(RevisarVentasCasoUso.class);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new RevisionVentasController(revisar)).build();
    }

    @Test
    @DisplayName("devuelve el resumen con título, explicación, gravedad, total y ejemplos de cada tipo")
    void devuelveElResumen() throws Exception {
        Hallazgo h = new Hallazgo(TipoHallazgo.PAGADO_SIN_VENTA, 2, null, null, "Pedido #2 (Entregado) por $300.00 no tiene venta.");
        when(revisar.revisar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).thenReturn(new ResultadoRevision(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), Instant.parse("2026-11-01T18:00:00Z"),
                List.of(new HallazgosDeUnTipo(TipoHallazgo.PAGADO_SIN_VENTA, 1, List.of(h)),
                        new HallazgosDeUnTipo(TipoHallazgo.STOCK_NEGATIVO, 0, List.of())),
                List.of(TipoHallazgo.COBRO_TARJETA_SIN_APLICAR)));

        mvc.perform(get("/v1/reportes/revision-ventas?desde=2026-10-01&hasta=2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalHallazgos").value(1))
                .andExpect(jsonPath("$.data.todoEnOrden").value(false))
                .andExpect(jsonPath("$.data.tipos[0].codigo").value("PAGADO_SIN_VENTA"))
                .andExpect(jsonPath("$.data.tipos[0].gravedad").value("ALTA"))
                .andExpect(jsonPath("$.data.tipos[0].titulo").value("Pedido pagado que no tiene venta"))
                .andExpect(jsonPath("$.data.tipos[0].ejemplos[0].pedidoId").value(2))
                .andExpect(jsonPath("$.data.tipos[1].total").value(0))
                .andExpect(jsonPath("$.data.noRevisados[0]").value("COBRO_TARJETA_SIN_APLICAR"));
    }

    @Test
    @DisplayName("sin fechas las pasa vacías al caso de uso (él pone las de por defecto)")
    void sinFechas() throws Exception {
        when(revisar.revisar(any(), any())).thenReturn(new ResultadoRevision(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 11, 1),
                Instant.parse("2026-11-01T18:00:00Z"), List.of(), List.of()));
        mvc.perform(get("/v1/reportes/revision-ventas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todoEnOrden").value(true));
        verify(revisar).revisar(null, null);
    }

    @Test
    @DisplayName("fechas al revés: 400 con el mensaje")
    void fechasAlReves() throws Exception {
        when(revisar.revisar(any(), any())).thenThrow(new RevisionVentasException.FechasInvalidas("La fecha «desde» no puede ser después de «hasta»."));
        mvc.perform(get("/v1/reportes/revision-ventas?desde=2026-11-01&hasta=2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La fecha «desde» no puede ser después de «hasta»."));
    }
}
