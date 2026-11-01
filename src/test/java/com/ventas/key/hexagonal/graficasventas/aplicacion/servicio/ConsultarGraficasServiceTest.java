package com.ventas.key.hexagonal.graficasventas.aplicacion.servicio;

import com.ventas.key.hexagonal.graficasventas.dominio.excepcion.GraficasVentasException;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.FormaDePagoVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.PuntoMensual;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasDelMes;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasPorFormaDePago;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarGraficasServiceTest {

    private GraficasPort puerto;
    private ConsultarGraficasService servicio;

    @BeforeEach
    void preparar() {
        puerto = mock(GraficasPort.class);
        // Hoy = 2026-11-15 (a mediodía UTC, para no cruzar de día en la zona de México)
        servicio = new ConsultarGraficasService(puerto, Clock.fixed(Instant.parse("2026-11-15T18:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("forma de pago: sin fechas toma del día 1 del mes a hoy")
    void fechasPorDefecto() {
        when(puerto.ventasPorFormaDePago(any(), any())).thenReturn(List.of());
        VentasPorFormaDePago r = servicio.ventasPorFormaDePago(null, null);
        assertThat(r.desde()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(r.hasta()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(r.total()).isZero();
        assertThat(r.formas()).isEmpty();
    }

    @Test
    @DisplayName("forma de pago: porcentajes sobre el total, de la que más vendió a la que menos, y la comisión sumada")
    void porcentajesYOrden() {
        when(puerto.ventasPorFormaDePago(any(), any())).thenReturn(List.of(
                new FormaDePagoVentas("Efectivo", 10, 1000.0, 0.0),
                new FormaDePagoVentas("Tarjeta crédito", 3, 2000.0, 87.0),
                new FormaDePagoVentas("Transferencia", 2, 1000.0, 0.0)));
        VentasPorFormaDePago r = servicio.ventasPorFormaDePago(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertThat(r.total()).isEqualTo(4000.0);
        assertThat(r.comisionTotal()).isEqualTo(87.0);
        assertThat(r.formas()).extracting(f -> f.forma()).containsExactly("Tarjeta crédito", "Efectivo", "Transferencia");
        assertThat(r.formas().get(0).porcentaje()).isEqualTo(50.0);
        assertThat(r.formas().get(1).porcentaje()).isEqualTo(25.0);
        assertThat(r.formas().get(0).comision()).isEqualTo(87.0);
    }

    @Test
    @DisplayName("forma de pago: fechas al revés se rechazan y no se consulta nada")
    void fechasAlReves() {
        assertThatThrownBy(() -> servicio.ventasPorFormaDePago(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(GraficasVentasException.class).hasMessageContaining("desde");
        verify(puerto, never()).ventasPorFormaDePago(any(), any());
    }

    @Test
    @DisplayName("por mes: siempre vienen todos los meses seguidos, del más viejo al actual, con ceros donde no hubo nada")
    void mesesSeguidosConCeros() {
        when(puerto.ventasPorMes(any(), any())).thenReturn(Map.of(
                YearMonth.of(2026, 11), new VentasDelMes(4, 1000.0, 300.0),
                YearMonth.of(2026, 9), new VentasDelMes(2, 500.0, 100.0)));
        when(puerto.gastosPorMes(any(), any())).thenReturn(Map.of(YearMonth.of(2026, 10), 80.0, YearMonth.of(2026, 11), 50.0));

        List<PuntoMensual> serie = servicio.ventasYGastosPorMes(4);

        assertThat(serie).extracting(PuntoMensual::mes).containsExactly("2026-08", "2026-09", "2026-10", "2026-11");
        assertThat(serie.get(0)).isEqualTo(new PuntoMensual("2026-08", 0, 0.0, 0.0, 0.0, 0.0));            // sin nada: ceros
        assertThat(serie.get(1).gananciaNeta()).isEqualTo(100.0);                                           // sin gastos
        assertThat(serie.get(2)).isEqualTo(new PuntoMensual("2026-10", 0, 0.0, 0.0, 80.0, -80.0));         // solo gastos: neta negativa
        assertThat(serie.get(3)).isEqualTo(new PuntoMensual("2026-11", 4, 1000.0, 300.0, 50.0, 250.0));    // neta = ganancia − gastos
    }

    @Test
    @DisplayName("por mes: la consulta va del día 1 del primer mes al último día del mes actual (cruza el año)")
    void rangoDeLaConsulta() {
        when(puerto.ventasPorMes(any(), any())).thenReturn(Map.of());
        when(puerto.gastosPorMes(any(), any())).thenReturn(Map.of());
        List<PuntoMensual> serie = servicio.ventasYGastosPorMes(12);
        assertThat(serie).hasSize(12);
        assertThat(serie.get(0).mes()).isEqualTo("2025-12");
        assertThat(serie.get(11).mes()).isEqualTo("2026-11");
        verify(puerto).ventasPorMes(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 11, 30));
        verify(puerto).gastosPorMes(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 11, 30));
    }

    @Test
    @DisplayName("por mes: menos de 1 o más de 24 meses se rechaza")
    void mesesFueraDeRango() {
        assertThatThrownBy(() -> servicio.ventasYGastosPorMes(0)).isInstanceOf(GraficasVentasException.class);
        assertThatThrownBy(() -> servicio.ventasYGastosPorMes(25)).isInstanceOf(GraficasVentasException.class);
        verify(puerto, never()).ventasPorMes(any(), any());
    }
}
