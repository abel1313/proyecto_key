package com.ventas.key.hexagonal.graficasventas.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.graficasventas.dominio.excepcion.GraficasVentasException;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CapitalParado;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.DineroPorCobrar;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CasoPorCobrar;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.EstadoDePedidos;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CategoriaPedido;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.TramoPorCobrar;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ClientesDelNegocio;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.EntregasPorZona;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.MapaDeVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CeldaDeCalor;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.TotalDeDia;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.MesDeClientes;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ClienteVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ParteDeEntregas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ParteDelTotal;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.PuntoMensual;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasPorFormaDePago;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.entrada.ConsultarGraficasCasoUso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GraficasVentasControllerTest {

    private final ConsultarGraficasCasoUso graficas = mock(ConsultarGraficasCasoUso.class);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new GraficasVentasController(graficas)).build();
    }

    @Test
    @DisplayName("forma de pago: devuelve el total, la comisión y cada forma con su porcentaje")
    void formaDePago() throws Exception {
        when(graficas.ventasPorFormaDePago(null, null)).thenReturn(new VentasPorFormaDePago(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15), 3000.0, 81.2,
                List.of(new ParteDelTotal("Tarjeta crédito", 1, 2000.0, 66.7, 81.2), new ParteDelTotal("Efectivo", 1, 1000.0, 33.3, 0.0))));
        mvc.perform(get("/v1/dashboard/graficas/ventas-por-forma-pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3000.0))
                .andExpect(jsonPath("$.data.comisionTotal").value(81.2))
                .andExpect(jsonPath("$.data.formas[0].forma").value("Tarjeta crédito"))
                .andExpect(jsonPath("$.data.formas[0].porcentaje").value(66.7));
    }

    @Test
    @DisplayName("por mes: 12 meses por defecto")
    void porMes() throws Exception {
        when(graficas.ventasYGastosPorMes(12)).thenReturn(List.of(new PuntoMensual("2026-11", 4, 1000.0, 300.0, 50.0, 250.0)));
        mvc.perform(get("/v1/dashboard/graficas/ventas-gastos-mensual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mes").value("2026-11"))
                .andExpect(jsonPath("$.data[0].gananciaNeta").value(250.0));
        verify(graficas).ventasYGastosPorMes(12);
    }

    @Test
    @DisplayName("meses fuera de rango y fechas al revés: 400 con el mensaje")
    void datosInvalidos() throws Exception {
        when(graficas.ventasYGastosPorMes(anyInt())).thenThrow(GraficasVentasException.mesesInvalidos(1, 24));
        mvc.perform(get("/v1/dashboard/graficas/ventas-gastos-mensual?meses=99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Los meses a mostrar deben ser de 1 a 24."));
    }

    @Test
    @DisplayName("por cobrar: total, tramos y los más antiguos")
    void porCobrar() throws Exception {
        when(graficas.dineroPorCobrar()).thenReturn(new DineroPorCobrar(700.0, 2,
                List.of(new TramoPorCobrar("0 a 7 días", 1, 200.0), new TramoPorCobrar("Más de 30 días", 1, 500.0)),
                List.of(new CasoPorCobrar(5, "Ana López", "Ir pagando", 500.0, 45))));
        mvc.perform(get("/v1/dashboard/graficas/por-cobrar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(700.0))
                .andExpect(jsonPath("$.data.tramos[1].etiqueta").value("Más de 30 días"))
                .andExpect(jsonPath("$.data.masAntiguos[0].cliente").value("Ana López"))
                .andExpect(jsonPath("$.data.masAntiguos[0].dias").value(45));
    }

    @Test
    @DisplayName("capital parado: 60 días por defecto; días inválidos dan 400")
    void capitalParado() throws Exception {
        when(graficas.capitalParado(60)).thenReturn(new CapitalParado(60, 1, 3, 300.0, List.of()));
        mvc.perform(get("/v1/dashboard/graficas/capital-parado")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capital").value(300.0)).andExpect(jsonPath("$.data.piezas").value(3));
        verify(graficas).capitalParado(60);

        when(graficas.capitalParado(3)).thenThrow(GraficasVentasException.diasInvalidos(7, 365));
        mvc.perform(get("/v1/dashboard/graficas/capital-parado?dias=3")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Los días a revisar deben ser de 7 a 365."));
    }

    @Test
    @DisplayName("pedidos por estado: 90 días por defecto, con atrasados y las seis categorías")
    void pedidosPorEstado() throws Exception {
        when(graficas.pedidosPorEstado(90)).thenReturn(EstadoDePedidos.de(LocalDate.of(2026, 8, 17), LocalDate.of(2026, 11, 15),
                java.util.Map.of(CategoriaPedido.PENDIENTE, 4), 2));
        mvc.perform(get("/v1/dashboard/graficas/pedidos-por-estado")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.atrasados").value(2))
                .andExpect(jsonPath("$.data.activos").value(4))
                .andExpect(jsonPath("$.data.estados.length()").value(6))
                .andExpect(jsonPath("$.data.estados[0].etiqueta").value("Pendiente de pago"));
    }

    @Test
    @DisplayName("cuándo se vende: 90 días por defecto, con celdas, días y el mejor día y hora")
    void cuandoSeVende() throws Exception {
        when(graficas.cuandoSeVende(90)).thenReturn(new MapaDeVentas(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 11, 15), 4, 1000.0,
                List.of(new CeldaDeCalor(3, 18, 1, 600.0)), List.of(new TotalDeDia(3, "Miércoles", 1, 600.0)), 3, 18));
        mvc.perform(get("/v1/dashboard/graficas/cuando-se-vende")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mejorDia").value(3)).andExpect(jsonPath("$.data.mejorHora").value(18))
                .andExpect(jsonPath("$.data.celdas[0].hora").value(18)).andExpect(jsonPath("$.data.porDia[0].nombre").value("Miércoles"));
    }

    @Test
    @DisplayName("clientes: 6 meses por defecto, con nuevos, recurrentes y los mejores")
    void clientes() throws Exception {
        when(graficas.clientes(6)).thenReturn(new ClientesDelNegocio(6, 3, 2, 920.0,
                List.of(new MesDeClientes("2026-11", 1, 1, 500.0, 70.0)), List.of(new ClienteVentas(2, "Luis Pérez", 1, 500.0))));
        mvc.perform(get("/v1/dashboard/graficas/clientes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientes").value(3)).andExpect(jsonPath("$.data.nuevos").value(2))
                .andExpect(jsonPath("$.data.recurrentes").value(1))
                .andExpect(jsonPath("$.data.porMes[0].mes").value("2026-11"))
                .andExpect(jsonPath("$.data.mejores[0].nombre").value("Luis Pérez"));
    }

    @Test
    @DisplayName("entregas por zona: 90 días por defecto")
    void entregasPorZona() throws Exception {
        when(graficas.entregasPorZona(90)).thenReturn(new EntregasPorZona(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 11, 15), 4, 420.0,
                List.of(new ParteDeEntregas("Centro", false, 2, 300.0, 50.0))));
        mvc.perform(get("/v1/dashboard/graficas/entregas-por-zona")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pedidos").value(4)).andExpect(jsonPath("$.data.zonas[0].zona").value("Centro"))
                .andExpect(jsonPath("$.data.zonas[0].porcentaje").value(50.0));
    }
}
