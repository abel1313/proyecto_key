package com.ventas.key.hexagonal.graficasventas.aplicacion.servicio;

import com.ventas.key.hexagonal.graficasventas.dominio.excepcion.GraficasVentasException;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ClienteVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ClientesDelNegocio;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CompraDeCliente;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.EntregasPorZona;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.MapaDeVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentaPorDiaYHora;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.ZonaDeEntrega;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.salida.GraficasPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarGraficasTercerasServiceTest {

    private GraficasPort puerto;
    private ConsultarGraficasService servicio;

    @BeforeEach
    void preparar() {
        puerto = mock(GraficasPort.class);
        servicio = new ConsultarGraficasService(puerto, Clock.fixed(Instant.parse("2026-11-15T18:00:00Z"), ZoneOffset.UTC));   // hoy = domingo 2026-11-15
    }

    @Test
    @DisplayName("cuándo se vende: suma por día de la semana y hora, y marca el mejor día y la mejor hora por dinero")
    void cuandoSeVende() {
        when(puerto.ventasPorDiaYHora(any(), any())).thenReturn(List.of(
                new VentaPorDiaYHora(LocalDate.of(2026, 11, 9), 10, 2, 300.0),    // lunes
                new VentaPorDiaYHora(LocalDate.of(2026, 11, 2), 10, 1, 100.0),    // otro lunes, misma hora
                new VentaPorDiaYHora(LocalDate.of(2026, 11, 11), 18, 1, 600.0))); // miércoles

        MapaDeVentas r = servicio.cuandoSeVende(90);

        verify(puerto).ventasPorDiaYHora(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 11, 15));   // 90 días contando hoy
        assertThat(r.ventas()).isEqualTo(4);
        assertThat(r.total()).isEqualTo(1000.0);
        assertThat(r.celdas()).hasSize(2);
        assertThat(r.celdas().get(0)).satisfies(c -> {
            assertThat(c.diaSemana()).isEqualTo(1);
            assertThat(c.hora()).isEqualTo(10);
            assertThat(c.ventas()).isEqualTo(3);
            assertThat(c.total()).isEqualTo(400.0);
        });
        assertThat(r.porDia()).hasSize(7);
        assertThat(r.porDia().get(0).nombre()).isEqualTo("Lunes");
        assertThat(r.porDia().get(6).total()).isZero();                       // domingo sin ventas
        assertThat(r.mejorDia()).isEqualTo(3);                                // miércoles: 600 contra lunes 400
        assertThat(r.mejorHora()).isEqualTo(18);
    }

    @Test
    @DisplayName("cuándo se vende: sin ventas no hay mejor día ni mejor hora")
    void sinVentas() {
        when(puerto.ventasPorDiaYHora(any(), any())).thenReturn(List.of());
        MapaDeVentas r = servicio.cuandoSeVende(30);
        assertThat(r.mejorDia()).isNull();
        assertThat(r.mejorHora()).isNull();
        assertThat(r.porDia()).hasSize(7);
    }

    @Test
    @DisplayName("clientes: separa nuevos de recurrentes por mes según su primera compra, y cuenta clientes distintos en el periodo")
    void nuevosYRecurrentes() {
        when(puerto.primerasCompras()).thenReturn(Map.of(
                1, LocalDate.of(2026, 9, 10),      // su primera compra fue en septiembre
                2, LocalDate.of(2026, 11, 3),      // en noviembre
                3, LocalDate.of(2025, 1, 1)));     // hace mucho
        when(puerto.comprasPorClienteYMes(any(), any())).thenReturn(List.of(
                new CompraDeCliente(1, YearMonth.of(2026, 9), 1, 100.0),
                new CompraDeCliente(1, YearMonth.of(2026, 10), 2, 200.0),
                new CompraDeCliente(3, YearMonth.of(2026, 10), 1, 50.0),
                new CompraDeCliente(2, YearMonth.of(2026, 11), 1, 500.0),
                new CompraDeCliente(3, YearMonth.of(2026, 11), 1, 70.0)));
        when(puerto.mejoresClientes(any(), any(), anyInt())).thenReturn(List.of(new ClienteVentas(2, "Luis Pérez", 1, 500.0)));

        ClientesDelNegocio r = servicio.clientes(3);

        verify(puerto).comprasPorClienteYMes(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30));
        assertThat(r.porMes()).extracting(m -> m.mes()).containsExactly("2026-09", "2026-10", "2026-11");
        assertThat(r.porMes().get(0)).satisfies(m -> { assertThat(m.nuevos()).isEqualTo(1); assertThat(m.recurrentes()).isZero(); assertThat(m.totalNuevos()).isEqualTo(100.0); });
        assertThat(r.porMes().get(1)).satisfies(m -> { assertThat(m.nuevos()).isZero(); assertThat(m.recurrentes()).isEqualTo(2); assertThat(m.totalRecurrentes()).isEqualTo(250.0); });
        assertThat(r.porMes().get(2)).satisfies(m -> { assertThat(m.nuevos()).isEqualTo(1); assertThat(m.recurrentes()).isEqualTo(1); });
        assertThat(r.clientes()).isEqualTo(3);                      // 1, 2 y 3, aunque el 1 y el 3 compraron en varios meses
        assertThat(r.nuevos()).isEqualTo(2);                        // el 1 y el 2 compraron por primera vez dentro del periodo
        assertThat(r.recurrentes()).isEqualTo(1);
        assertThat(r.total()).isEqualTo(920.0);
        assertThat(r.mejores()).extracting(ClienteVentas::nombre).containsExactly("Luis Pérez");
    }

    @Test
    @DisplayName("clientes: meses sin compras salen en cero, y de 1 a 12 meses nada más")
    void mesesSinCompras() {
        when(puerto.primerasCompras()).thenReturn(Map.of());
        when(puerto.comprasPorClienteYMes(any(), any())).thenReturn(List.of());
        when(puerto.mejoresClientes(any(), any(), anyInt())).thenReturn(List.of());
        ClientesDelNegocio r = servicio.clientes(6);
        assertThat(r.porMes()).hasSize(6).allMatch(m -> m.nuevos() == 0 && m.recurrentes() == 0);
        assertThat(r.clientes()).isZero();

        assertThatThrownBy(() -> servicio.clientes(0)).isInstanceOf(GraficasVentasException.class);
        assertThatThrownBy(() -> servicio.clientes(13)).isInstanceOf(GraficasVentasException.class);
    }

    @Test
    @DisplayName("entregas por zona: del lugar con más pedidos al que menos, con porcentajes sobre el total")
    void entregasPorZona() {
        when(puerto.pedidosPorZona(any(), any())).thenReturn(List.of(
                new ZonaDeEntrega("Recoger en tienda", true, 1, 50.0),
                new ZonaDeEntrega("Centro", false, 2, 300.0),
                new ZonaDeEntrega("Sin lugar de entrega", false, 1, 70.0)));

        EntregasPorZona r = servicio.entregasPorZona(90);

        verify(puerto).pedidosPorZona(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 11, 15));
        assertThat(r.pedidos()).isEqualTo(4);
        assertThat(r.total()).isEqualTo(420.0);
        assertThat(r.zonas()).extracting(z -> z.zona()).containsExactly("Centro", "Recoger en tienda", "Sin lugar de entrega");  // empate: por nombre
        assertThat(r.zonas().get(0).porcentaje()).isEqualTo(50.0);
        assertThat(r.zonas().get(1).recogerEnTienda()).isTrue();
    }

    @Test
    @DisplayName("días fuera de 7–365: se rechaza y no se consulta nada")
    void diasInvalidos() {
        assertThatThrownBy(() -> servicio.cuandoSeVende(3)).isInstanceOf(GraficasVentasException.class);
        assertThatThrownBy(() -> servicio.entregasPorZona(400)).isInstanceOf(GraficasVentasException.class);
        verify(puerto, never()).ventasPorDiaYHora(any(), any());
        verify(puerto, never()).pedidosPorZona(any(), any());
    }
}
